import 'package:coford_app/data/database.dart';
import 'package:coford_app/data/repository.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late AppDatabase db;
  late CofordRepository repo;

  setUp(() {
    db = AppDatabase.forTesting(NativeDatabase.memory());
    repo = CofordRepository(db);
  });

  tearDown(() async => db.close());

  test('Tạo đơn và thanh toán cập nhật trạng thái + báo cáo', () async {
    final catId = await repo.createCategory('Cà phê', 1);
    final itemId = await repo.createMenuItem(
        categoryId: catId, name: 'Cà phê sữa', price: 25000);
    final item =
        await (db.select(db.menuItems)..where((t) => t.id.equals(itemId)))
            .getSingle();

    final orderId = await repo.createOrder(
      tableNo: 'B1',
      lines: [CartLine(item, quantity: 2)],
    );

    var order = await repo.getOrder(orderId);
    expect(order.order.totalAmount, 50000);
    expect(order.order.status, 'NEW');

    await repo.payOrder(orderId, 'CASH');
    order = await repo.getOrder(orderId);
    expect(order.order.status, 'PAID');

    final rev = await repo.dailyRevenue(7);
    expect(rev.last.revenue, 50000);

    final top = await repo.topItems(7);
    expect(top.first.totalQuantity, 2);
  });

  test('Hủy đơn đã thanh toán thì báo lỗi', () async {
    final catId = await repo.createCategory('Trà', 1);
    final itemId = await repo.createMenuItem(
        categoryId: catId, name: 'Trà đào', price: 35000);
    final item =
        await (db.select(db.menuItems)..where((t) => t.id.equals(itemId)))
            .getSingle();
    final orderId =
        await repo.createOrder(tableNo: 'B2', lines: [CartLine(item)]);
    await repo.payOrder(orderId, 'CASH');
    expect(() => repo.cancelOrder(orderId), throwsStateError);
  });
}
