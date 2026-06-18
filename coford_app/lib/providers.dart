import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'data/database.dart';
import 'data/repository.dart';

final databaseProvider = Provider<AppDatabase>((ref) {
  final db = AppDatabase();
  ref.onDispose(db.close);
  return db;
});

final repositoryProvider = Provider<CofordRepository>((ref) {
  return CofordRepository(ref.watch(databaseProvider));
});

// ----- Menu -----
final categoriesProvider = StreamProvider<List<Category>>((ref) {
  return ref.watch(repositoryProvider).watchCategories();
});

class MenuQuery {
  final int? categoryId;
  final bool onlyAvailable;
  const MenuQuery({this.categoryId, this.onlyAvailable = false});

  @override
  bool operator ==(Object other) =>
      other is MenuQuery &&
      other.categoryId == categoryId &&
      other.onlyAvailable == onlyAvailable;

  @override
  int get hashCode => Object.hash(categoryId, onlyAvailable);
}

final menuItemsProvider =
    StreamProvider.family<List<MenuItem>, MenuQuery>((ref, q) {
  return ref
      .watch(repositoryProvider)
      .watchMenuItems(categoryId: q.categoryId, onlyAvailable: q.onlyAvailable);
});

// ----- Đơn hàng -----
final ordersProvider =
    StreamProvider.family<List<Order>, String?>((ref, status) {
  return ref.watch(repositoryProvider).watchOrders(status: status);
});

final orderProvider =
    StreamProvider.family<OrderWithItems?, int>((ref, id) {
  return ref.watch(repositoryProvider).watchOrder(id);
});

// ----- Bếp -----
final kitchenProvider = StreamProvider<List<Order>>((ref) {
  return ref.watch(repositoryProvider).watchKitchen();
});

// ----- Cấu hình -----
final settingsProvider = StreamProvider<Map<String, String?>>((ref) {
  return ref.watch(repositoryProvider).watchSettings().map(
        (rows) => {for (final s in rows) s.key: s.value},
      );
});

// ----- Báo cáo (StreamProvider để tự cập nhật khi có giao dịch) -----
final dailyRevenueProvider =
    StreamProvider.family<List<DailyRevenue>, int>((ref, days) {
  return ref.watch(repositoryProvider).watchDailyRevenue(days);
});

final topItemsProvider =
    StreamProvider.family<List<TopItem>, int>((ref, days) {
  return ref.watch(repositoryProvider).watchTopItems(days);
});
