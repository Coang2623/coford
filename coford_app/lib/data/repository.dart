import 'package:drift/drift.dart';

import 'database.dart';

/// Một dòng trong giỏ hàng trước khi gửi đơn.
class CartLine {
  final MenuItem item;
  int quantity;
  String? note;
  CartLine(this.item, {this.quantity = 1, this.note});

  int get lineTotal => item.price * quantity;
}

/// Đơn kèm danh sách món (đọc).
class OrderWithItems {
  final Order order;
  final List<OrderItem> items;
  OrderWithItems(this.order, this.items);
  int get itemCount => items.fold(0, (s, i) => s + i.quantity);
}

class CofordRepository {
  final AppDatabase db;
  CofordRepository(this.db);

  // ---------- MENU ----------
  Stream<List<Category>> watchCategories() {
    final q = db.select(db.categories)
      ..orderBy([(c) => OrderingTerm(expression: c.sortOrder)]);
    return q.watch();
  }

  Stream<List<MenuItem>> watchMenuItems({int? categoryId, bool onlyAvailable = false}) {
    final q = db.select(db.menuItems);
    if (categoryId != null) {
      q.where((t) => t.categoryId.equals(categoryId));
    }
    if (onlyAvailable) {
      q.where((t) => t.available.equals(true));
    }
    q.orderBy([(t) => OrderingTerm(expression: t.name)]);
    return q.watch();
  }

  Future<int> createCategory(String name, int sortOrder) {
    return db.into(db.categories).insert(
          CategoriesCompanion.insert(name: name, sortOrder: Value(sortOrder)),
        );
  }

  Future<void> updateCategory(int id, {String? name, int? sortOrder}) {
    return (db.update(db.categories)..where((c) => c.id.equals(id))).write(
      CategoriesCompanion(
        name: name == null ? const Value.absent() : Value(name),
        sortOrder: sortOrder == null ? const Value.absent() : Value(sortOrder),
      ),
    );
  }

  Future<void> deleteCategory(int id) {
    return (db.delete(db.categories)..where((c) => c.id.equals(id))).go();
  }

  Future<int> createMenuItem({
    required int categoryId,
    required String name,
    String? description,
    required int price,
    bool available = true,
  }) {
    return db.into(db.menuItems).insert(MenuItemsCompanion.insert(
          categoryId: categoryId,
          name: name,
          description: Value(description),
          price: price,
          available: Value(available),
        ));
  }

  Future<void> updateMenuItem(
    int id, {
    int? categoryId,
    String? name,
    String? description,
    int? price,
    bool? available,
  }) {
    return (db.update(db.menuItems)..where((t) => t.id.equals(id))).write(
      MenuItemsCompanion(
        categoryId: categoryId == null ? const Value.absent() : Value(categoryId),
        name: name == null ? const Value.absent() : Value(name),
        description: description == null ? const Value.absent() : Value(description),
        price: price == null ? const Value.absent() : Value(price),
        available: available == null ? const Value.absent() : Value(available),
      ),
    );
  }

  Future<void> deleteMenuItem(int id) {
    return (db.delete(db.menuItems)..where((t) => t.id.equals(id))).go();
  }

  // ---------- ĐƠN HÀNG ----------
  Stream<List<Order>> watchOrders({String? status}) {
    final q = db.select(db.orders);
    if (status != null) q.where((o) => o.status.equals(status));
    q.orderBy([(o) => OrderingTerm(expression: o.createdAt, mode: OrderingMode.desc)]);
    return q.watch();
  }

  Stream<OrderWithItems?> watchOrder(int id) {
    final orderQ = db.select(db.orders)..where((o) => o.id.equals(id));
    return orderQ.watchSingleOrNull().asyncMap((order) async {
      if (order == null) return null;
      final items = await (db.select(db.orderItems)
            ..where((i) => i.orderId.equals(id)))
          .get();
      return OrderWithItems(order, items);
    });
  }

  Future<OrderWithItems> getOrder(int id) async {
    final order = await (db.select(db.orders)..where((o) => o.id.equals(id))).getSingle();
    final items =
        await (db.select(db.orderItems)..where((i) => i.orderId.equals(id))).get();
    return OrderWithItems(order, items);
  }

  /// Tạo đơn mới từ giỏ hàng. Trả về id đơn.
  Future<int> createOrder({
    required String tableNo,
    String? note,
    required List<CartLine> lines,
  }) {
    return db.transaction(() async {
      final total = lines.fold<int>(0, (s, l) => s + l.lineTotal);
      final orderId = await db.into(db.orders).insert(OrdersCompanion.insert(
            tableNo: tableNo,
            note: Value(note),
            totalAmount: Value(total),
          ));
      for (final l in lines) {
        await db.into(db.orderItems).insert(OrderItemsCompanion.insert(
              orderId: orderId,
              menuItemId: l.item.id,
              itemName: l.item.name, // snapshot
              unitPrice: l.item.price, // snapshot
              quantity: l.quantity,
              lineTotal: l.lineTotal,
              note: Value(l.note),
            ));
      }
      return orderId;
    });
  }

  /// Thay toàn bộ danh sách món của đơn NEW.
  Future<void> updateOrderItems(int orderId, List<CartLine> lines) {
    return db.transaction(() async {
      final order =
          await (db.select(db.orders)..where((o) => o.id.equals(orderId))).getSingle();
      if (order.status != 'NEW') {
        throw StateError('Chỉ sửa được đơn đang chờ thanh toán');
      }
      await (db.delete(db.orderItems)..where((i) => i.orderId.equals(orderId))).go();
      final total = lines.fold<int>(0, (s, l) => s + l.lineTotal);
      for (final l in lines) {
        await db.into(db.orderItems).insert(OrderItemsCompanion.insert(
              orderId: orderId,
              menuItemId: l.item.id,
              itemName: l.item.name,
              unitPrice: l.item.price,
              quantity: l.quantity,
              lineTotal: l.lineTotal,
              note: Value(l.note),
            ));
      }
      await (db.update(db.orders)..where((o) => o.id.equals(orderId))).write(
        OrdersCompanion(totalAmount: Value(total), updatedAt: Value(DateTime.now())),
      );
    });
  }

  Future<void> cancelOrder(int orderId) async {
    final order =
        await (db.select(db.orders)..where((o) => o.id.equals(orderId))).getSingle();
    if (order.status != 'NEW') {
      throw StateError('Chỉ hủy được đơn đang chờ thanh toán');
    }
    await (db.update(db.orders)..where((o) => o.id.equals(orderId))).write(
      OrdersCompanion(status: const Value('CANCELLED'), updatedAt: Value(DateTime.now())),
    );
  }

  Future<void> setPrepared(int orderId, bool prepared) {
    return (db.update(db.orders)..where((o) => o.id.equals(orderId))).write(
      OrdersCompanion(prepared: Value(prepared), updatedAt: Value(DateTime.now())),
    );
  }

  // ---------- THANH TOÁN ----------
  Future<void> payOrder(int orderId, String method) {
    return db.transaction(() async {
      final order =
          await (db.select(db.orders)..where((o) => o.id.equals(orderId))).getSingle();
      if (order.status == 'PAID') return; // idempotent
      if (order.status != 'NEW') {
        throw StateError('Đơn không ở trạng thái thanh toán được');
      }
      await db.into(db.payments).insert(PaymentsCompanion.insert(
            orderId: orderId,
            method: method,
            amount: order.totalAmount,
          ));
      await (db.update(db.orders)..where((o) => o.id.equals(orderId))).write(
        OrdersCompanion(status: const Value('PAID'), updatedAt: Value(DateTime.now())),
      );
    });
  }

  Future<Payment?> getPayment(int orderId) {
    return (db.select(db.payments)..where((p) => p.orderId.equals(orderId)))
        .getSingleOrNull();
  }

  // ---------- BẾP ----------
  Stream<List<Order>> watchKitchen() {
    final q = db.select(db.orders)
      ..where((o) => o.prepared.equals(false) & o.status.equals('NEW'))
      ..orderBy([(o) => OrderingTerm(expression: o.createdAt)]);
    return q.watch();
  }

  // ---------- BÁO CÁO ----------
  /// Stream tự tính lại mỗi khi bảng orders thay đổi (tạo/thanh toán/hủy).
  /// Dùng orders làm "trigger" vì khi thanh toán, orders được cập nhật cùng
  /// transaction với việc thêm payment.
  Stream<List<DailyRevenue>> watchDailyRevenue(int days) {
    return db.select(db.orders).watch().asyncMap((_) => dailyRevenue(days));
  }

  Stream<List<TopItem>> watchTopItems(int days, {int limit = 5}) {
    return db.select(db.orders).watch().asyncMap((_) => topItems(days, limit: limit));
  }

  /// Doanh thu theo ngày trong [days] ngày gần nhất (chỉ tính đơn PAID).
  Future<List<DailyRevenue>> dailyRevenue(int days) async {
    final since = DateTime.now().subtract(Duration(days: days - 1));
    final start = DateTime(since.year, since.month, since.day);
    final payments = await (db.select(db.payments)
          ..where((p) => p.paidAt.isBiggerOrEqualValue(start)))
        .get();
    final map = <String, DailyRevenue>{};
    for (final p in payments) {
      final d = p.paidAt.toLocal();
      final key = '${d.year}-${d.month}-${d.day}';
      final existing = map[key];
      final day = DateTime(d.year, d.month, d.day);
      if (existing == null) {
        map[key] = DailyRevenue(day, p.amount, 1);
      } else {
        map[key] = DailyRevenue(day, existing.revenue + p.amount, existing.orderCount + 1);
      }
    }
    // điền đủ các ngày (kể cả ngày 0 đồng)
    final result = <DailyRevenue>[];
    for (int i = 0; i < days; i++) {
      final d = DateTime(start.year, start.month, start.day + i);
      final key = '${d.year}-${d.month}-${d.day}';
      result.add(map[key] ?? DailyRevenue(d, 0, 0));
    }
    return result;
  }

  Future<List<TopItem>> topItems(int days, {int limit = 5}) async {
    final since = DateTime.now().subtract(Duration(days: days - 1));
    final start = DateTime(since.year, since.month, since.day);
    // join order_items với orders PAID
    final query = db.select(db.orderItems).join([
      innerJoin(db.orders, db.orders.id.equalsExp(db.orderItems.orderId)),
    ])
      ..where(db.orders.status.equals('PAID') &
          db.orders.updatedAt.isBiggerOrEqualValue(start));
    final rows = await query.get();
    final map = <String, TopItem>{};
    for (final r in rows) {
      final oi = r.readTable(db.orderItems);
      final existing = map[oi.itemName];
      if (existing == null) {
        map[oi.itemName] = TopItem(oi.itemName, oi.quantity, oi.lineTotal);
      } else {
        map[oi.itemName] = TopItem(
          oi.itemName,
          existing.totalQuantity + oi.quantity,
          existing.totalAmount + oi.lineTotal,
        );
      }
    }
    final list = map.values.toList()
      ..sort((a, b) => b.totalQuantity.compareTo(a.totalQuantity));
    return list.take(limit).toList();
  }

  // ---------- CẤU HÌNH (Settings) ----------
  Future<String?> getSetting(String key) async {
    final row =
        await (db.select(db.settings)..where((s) => s.key.equals(key))).getSingleOrNull();
    return row?.value;
  }

  Stream<List<Setting>> watchSettings() => db.select(db.settings).watch();

  Future<void> setSetting(String key, String? value) {
    return db.into(db.settings).insertOnConflictUpdate(
          SettingsCompanion(key: Value(key), value: Value(value)),
        );
  }
}

class DailyRevenue {
  final DateTime date;
  final int revenue;
  final int orderCount;
  DailyRevenue(this.date, this.revenue, this.orderCount);
}

class TopItem {
  final String name;
  final int totalQuantity;
  final int totalAmount;
  TopItem(this.name, this.totalQuantity, this.totalAmount);
}
