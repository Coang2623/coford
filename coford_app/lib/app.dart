import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'screens/kitchen_page.dart';
import 'screens/menu_admin_page.dart';
import 'screens/order_page.dart';
import 'screens/orders_page.dart';
import 'screens/report_page.dart';
import 'theme/app_theme.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  static const _tabs = [
    _TabDef('Bán hàng', CupertinoIcons.plus_circle_fill),
    _TabDef('Đơn hàng', CupertinoIcons.list_bullet),
    _TabDef('Bếp', CupertinoIcons.flame_fill),
    _TabDef('Báo cáo', CupertinoIcons.chart_bar_alt_fill),
    _TabDef('Quản lý', CupertinoIcons.square_grid_2x2_fill),
  ];

  final _pages = const [
    OrderPage(),
    OrdersPage(),
    KitchenPage(),
    ReportPage(),
    MenuAdminPage(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _pages),
      bottomNavigationBar: _BottomBar(
        tabs: _tabs,
        index: _index,
        onTap: (i) => setState(() => _index = i),
      ),
    );
  }
}

class _TabDef {
  final String label;
  final IconData icon;
  const _TabDef(this.label, this.icon);
}

/// Tab bar dưới kiểu iOS: nền sáng phẳng + đường kẻ mảnh phía trên (không blur).
class _BottomBar extends StatelessWidget {
  final List<_TabDef> tabs;
  final int index;
  final ValueChanged<int> onTap;
  const _BottomBar(
      {required this.tabs, required this.index, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.card,
        border: Border(top: BorderSide(color: AppColors.separator, width: 0.5)),
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 56,
          child: Row(
            children: [
              for (int i = 0; i < tabs.length; i++)
                Expanded(
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () => onTap(i),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(tabs[i].icon,
                            size: 24,
                            color: i == index
                                ? AppColors.accent
                                : AppColors.tertiaryLabel),
                        const SizedBox(height: 3),
                        Text(tabs[i].label,
                            style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.w600,
                                color: i == index
                                    ? AppColors.accent
                                    : AppColors.tertiaryLabel)),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Khung trang chuẩn: nền hệ thống + large title kiểu iOS.
class IosScaffold extends StatelessWidget {
  final String title;
  final Widget child;
  final List<Widget>? actions;
  final Widget? leading;
  final Widget? floatingBottom;
  const IosScaffold({
    super.key,
    required this.title,
    required this.child,
    this.actions,
    this.leading,
    this.floatingBottom,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 12, 12, 8),
              child: Row(
                children: [
                  if (leading != null) ...[leading!, const SizedBox(width: 8)],
                  Expanded(child: Text(title, style: AppText.largeTitle)),
                  if (actions != null) ...actions!,
                ],
              ),
            ),
            Expanded(child: child),
          ],
        ),
      ),
      bottomSheet: floatingBottom,
    );
  }
}
