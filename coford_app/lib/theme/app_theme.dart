import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

/// Bảng màu & theme theo tinh thần iOS 26 (KHÔNG dùng liquid glass):
/// nền phẳng sạch, thẻ bo góc, bóng đổ nhẹ, typography kiểu SF.
class AppColors {
  // Accent động thay đổi theo cấu hình
  static Color accent = const Color(0xFF8A5A2B);
  static Color accentSoft = const Color(0xFFF2E8DD);

  // Nền hệ thống (iOS systemGroupedBackground sáng)
  static const Color background = Color(0xFFF2F2F7);
  static const Color card = Color(0xFFFFFFFF);
  static const Color cardAlt = Color(0xFFFBFBFD);

  static const Color label = Color(0xFF1C1C1E);
  static const Color secondaryLabel = Color(0xFF6E6E73);
  static const Color tertiaryLabel = Color(0xFFAEAEB2);
  static const Color separator = Color(0xFFE5E5EA);

  static const Color green = Color(0xFF34C759);
  static const Color red = Color(0xFFFF3B30);
  static const Color orange = Color(0xFFFF9500);
  static const Color blue = Color(0xFF007AFF);
  static const Color fill = Color(0xFFEFEFF4);

  // Danh sách chủ đề màu sắc
  static const Map<String, (Color, Color, String)> themes = {
    'espresso': (Color(0xFF8A5A2B), Color(0xFFF2E8DD), 'Espresso (Ấm áp)'),
    'matcha': (Color(0xFF2E7D32), Color(0xFFE8F5E9), 'Matcha (Thanh mát)'),
    'ocean': (Color(0xFF007AFF), Color(0xFFE5F1FF), 'Ocean Blue (Mát mẻ)'),
    'rose': (Color(0xFFD81B60), Color(0xFFFCE4EC), 'Rose Berry (Ngọt ngào)'),
    'charcoal': (Color(0xFF37474F), Color(0xFFECEFF1), 'Charcoal (Sang trọng)'),
  };

  static void updateTheme(String key) {
    final t = themes[key] ?? themes['espresso']!;
    accent = t.$1;
    accentSoft = t.$2;
  }
}

class AppRadius {
  static const double sm = 10;
  static const double md = 14;
  static const double lg = 20;
  static const double xl = 28;
}

/// Bóng đổ rất nhẹ, kiểu iOS (không phải neumorphism).
const List<BoxShadow> kCardShadow = [
  BoxShadow(
    color: Color(0x0F000000),
    blurRadius: 12,
    offset: Offset(0, 4),
  ),
];

ThemeData buildAppTheme() {
  const fontFamily = '.SF Pro Text'; // dùng font hệ thống nếu có, fallback mặc định
  final base = ThemeData(
    useMaterial3: true,
    scaffoldBackgroundColor: AppColors.background,
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.accent,
      primary: AppColors.accent,
      surface: AppColors.card,
      brightness: Brightness.light,
    ),
    splashFactory: InkRipple.splashFactory,
  );

  return base.copyWith(
    textTheme: base.textTheme.apply(
      bodyColor: AppColors.label,
      displayColor: AppColors.label,
      fontFamily: fontFamily,
    ),
    cupertinoOverrideTheme: CupertinoThemeData(
      primaryColor: AppColors.accent,
    ),
  );
}

// ----- Text styles tiện dùng (kiểu iOS) -----
class AppText {
  static const TextStyle largeTitle = TextStyle(
    fontSize: 34,
    fontWeight: FontWeight.w700,
    letterSpacing: 0.37,
    color: AppColors.label,
  );
  static const TextStyle title = TextStyle(
    fontSize: 22,
    fontWeight: FontWeight.w700,
    color: AppColors.label,
  );
  static const TextStyle headline = TextStyle(
    fontSize: 17,
    fontWeight: FontWeight.w600,
    color: AppColors.label,
  );
  static const TextStyle body = TextStyle(
    fontSize: 17,
    fontWeight: FontWeight.w400,
    color: AppColors.label,
  );
  static const TextStyle subhead = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w400,
    color: AppColors.secondaryLabel,
  );
  static const TextStyle footnote = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w400,
    color: AppColors.secondaryLabel,
  );
  static const TextStyle caption = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w500,
    color: AppColors.tertiaryLabel,
  );
}
