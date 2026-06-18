import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

part 'database.g.dart';

// ----- Bảng (Tables) -----

class Categories extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text().withLength(min: 1, max: 100)();
  IntColumn get sortOrder => integer().withDefault(const Constant(0))();
}

class MenuItems extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get categoryId =>
      integer().references(Categories, #id, onDelete: KeyAction.cascade)();
  TextColumn get name => text().withLength(min: 1, max: 150)();
  TextColumn get description => text().nullable()();
  // Giá lưu theo đồng (số nguyên VND)
  IntColumn get price => integer()();
  BoolColumn get available => boolean().withDefault(const Constant(true))();
  DateTimeColumn get createdAt =>
      dateTime().withDefault(currentDateAndTime)();
}

class Orders extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get tableNo => text().withLength(min: 1, max: 20)();
  // NEW | PAID | CANCELLED
  TextColumn get status =>
      text().withLength(max: 20).withDefault(const Constant('NEW'))();
  IntColumn get totalAmount => integer().withDefault(const Constant(0))();
  TextColumn get note => text().nullable()();
  BoolColumn get prepared => boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();
  DateTimeColumn get updatedAt => dateTime().withDefault(currentDateAndTime)();
}

class OrderItems extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get orderId =>
      integer().references(Orders, #id, onDelete: KeyAction.cascade)();
  IntColumn get menuItemId => integer()();
  // Snapshot tên + giá lúc đặt
  TextColumn get itemName => text().withLength(min: 1, max: 150)();
  IntColumn get unitPrice => integer()();
  IntColumn get quantity => integer()();
  IntColumn get lineTotal => integer()();
  TextColumn get note => text().nullable()();
}

class Payments extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get orderId => integer().unique()();
  // CASH | CARD | TRANSFER
  TextColumn get method => text().withLength(max: 20)();
  IntColumn get amount => integer()();
  DateTimeColumn get paidAt => dateTime().withDefault(currentDateAndTime)();
}

// Cấu hình dạng key/value (QR chuyển khoản, tên quán...)
class Settings extends Table {
  TextColumn get key => text()();
  TextColumn get value => text().nullable()();

  @override
  Set<Column> get primaryKey => {key};
}

// ----- Database -----

@DriftDatabase(
  tables: [Categories, MenuItems, Orders, OrderItems, Payments, Settings],
)
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_open());
  AppDatabase.forTesting(super.e);

  @override
  int get schemaVersion => 1;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) async {
          await m.createAll();
        },
        beforeOpen: (details) async {
          await customStatement('PRAGMA foreign_keys = ON');
        },
      );

  static QueryExecutor _open() {
    // drift_flutter: native dùng sqlite3, web dùng WASM.
    // Các file sqlite3.wasm + drift_worker.js đặt sẵn trong thư mục web/.
    return driftDatabase(
      name: 'coford_db',
      web: DriftWebOptions(
        sqlite3Wasm: Uri.parse('sqlite3.wasm'),
        driftWorker: Uri.parse('drift_worker.js'),
      ),
    );
  }
}
