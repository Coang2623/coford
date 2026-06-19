import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'data/database.dart';
import 'data/seed.dart';
import 'providers.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final db = AppDatabase();
  await seedIfEmpty(db);

  runApp(
    ProviderScope(
      overrides: [databaseProvider.overrideWithValue(db)],
      child: const CofordApp(),
    ),
  );
}

class CofordApp extends ConsumerWidget {
  const CofordApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsProvider).value ?? {};
    final themeKey = settings['theme_color'] ?? 'espresso';

    // Cập nhật bảng màu trước khi dựng ThemeData
    AppColors.updateTheme(themeKey);

    return MaterialApp(
      title: 'Coford',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      home: const HomeShell(),
    );
  }
}
