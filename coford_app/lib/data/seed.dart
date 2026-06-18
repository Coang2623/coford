import 'package:drift/drift.dart';

import 'database.dart';

/// Nạp dữ liệu mẫu lần đầu chạy (menu + cấu hình QR).
Future<void> seedIfEmpty(AppDatabase db) async {
  final count = await db.managers.categories.count();
  if (count > 0) return;

  await db.transaction(() async {
    Future<int> cat(String name, int order) => db.into(db.categories).insert(
        CategoriesCompanion.insert(name: name, sortOrder: Value(order)));

    Future<void> item(int catId, String name, int price, [String? desc]) =>
        db.into(db.menuItems).insert(MenuItemsCompanion.insert(
              categoryId: catId,
              name: name,
              price: price,
              description: Value(desc),
            ));

    final coffee = await cat('Cà phê', 1);
    final tea = await cat('Trà', 2);
    final ice = await cat('Đá xay', 3);
    final cake = await cat('Bánh', 4);

    await item(coffee, 'Cà phê đen', 20000, 'Đậm đà truyền thống');
    await item(coffee, 'Cà phê sữa', 25000, 'Béo ngậy');
    await item(coffee, 'Bạc xỉu', 30000, 'Nhiều sữa, ít cà phê');
    await item(coffee, 'Cappuccino', 40000, 'Bọt sữa mịn');
    await item(coffee, 'Latte', 45000, 'Thơm sữa');

    await item(tea, 'Trà đào', 35000, 'Đào miếng tươi');
    await item(tea, 'Trà vải', 35000);
    await item(tea, 'Trà sen vàng', 38000);
    await item(tea, 'Trà tắc', 25000);

    await item(ice, 'Cookie đá xay', 50000);
    await item(ice, 'Matcha đá xay', 55000);
    await item(ice, 'Chocolate đá xay', 52000);

    await item(cake, 'Bánh tiramisu', 35000);
    await item(cake, 'Bánh phô mai', 32000);
    await item(cake, 'Croissant', 28000);

    // Cấu hình mặc định
    await db.into(db.settings).insert(
        const SettingsCompanion(key: Value('shop_name'), value: Value('Coford Coffee')));
    await db.into(db.settings).insert(
        const SettingsCompanion(key: Value('bank_name'), value: Value('Vietcombank')));
    await db.into(db.settings).insert(const SettingsCompanion(
        key: Value('bank_account_no'), value: Value('0123456789')));
    await db.into(db.settings).insert(const SettingsCompanion(
        key: Value('bank_account_name'), value: Value('COFORD COFFEE')));
  });
}
