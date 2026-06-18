import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../app.dart';
import '../data/database.dart';
import '../data/repository.dart';
import '../providers.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/ui.dart';

const _tables = ['B1', 'B2', 'B3', 'B4', 'B5', 'B6', 'Mang về'];

class OrderPage extends ConsumerStatefulWidget {
  const OrderPage({super.key});

  @override
  ConsumerState<OrderPage> createState() => _OrderPageState();
}

class _OrderPageState extends ConsumerState<OrderPage> {
  String _table = _tables.first;
  int? _categoryId; // null = tất cả
  final Map<int, CartLine> _cart = {};
  bool _submitting = false;

  int get _total => _cart.values.fold(0, (s, l) => s + l.lineTotal);
  int get _count => _cart.values.fold(0, (s, l) => s + l.quantity);

  void _add(MenuItem item) {
    setState(() {
      final existing = _cart[item.id];
      if (existing != null) {
        existing.quantity++;
      } else {
        _cart[item.id] = CartLine(item, quantity: 1);
      }
    });
  }

  void _setQty(int itemId, int qty) {
    setState(() {
      if (qty <= 0) {
        _cart.remove(itemId);
      } else {
        _cart[itemId]!.quantity = qty;
      }
    });
  }

  Future<void> _submit() async {
    if (_cart.isEmpty) return;
    setState(() => _submitting = true);
    try {
      await ref.read(repositoryProvider).createOrder(
            tableNo: _table,
            lines: _cart.values.toList(),
          );
      if (!mounted) return;
      setState(() {
        _cart.clear();
        _submitting = false;
      });
      Navigator.of(context).maybePop();
      showToast(context, 'Đã gửi đơn cho bàn $_table');
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      showToast(context, 'Lỗi gửi đơn: $e', error: true);
    }
  }

  void _openCart() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.background,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.lg)),
      ),
      builder: (_) => _CartSheet(
        cart: _cart,
        table: _table,
        total: _total,
        submitting: _submitting,
        onQty: _setQty,
        onSubmit: _submit,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final categoriesAsync = ref.watch(categoriesProvider);
    final menuAsync = ref.watch(
      menuItemsProvider(MenuQuery(categoryId: _categoryId, onlyAvailable: true)),
    );

    return IosScaffold(
      title: 'Bán hàng',
      child: Column(
        children: [
          // Chọn bàn
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: AppCard(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              child: Row(
                children: [
                  const Icon(CupertinoIcons.table, color: AppColors.accent),
                  const SizedBox(width: 10),
                  const Text('Bàn', style: AppText.headline),
                  const Spacer(),
                  DropdownButton<String>(
                    value: _table,
                    underline: const SizedBox(),
                    borderRadius: BorderRadius.circular(AppRadius.sm),
                    items: [
                      for (final t in _tables)
                        DropdownMenuItem(value: t, child: Text(t)),
                    ],
                    onChanged: (v) => setState(() => _table = v!),
                  ),
                ],
              ),
            ),
          ),
          // Bộ lọc danh mục
          categoriesAsync.when(
            data: (cats) => Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    _CategoryChip(
                      label: 'Tất cả',
                      selected: _categoryId == null,
                      onTap: () => setState(() => _categoryId = null),
                    ),
                    for (final c in cats)
                      _CategoryChip(
                        label: c.name,
                        selected: _categoryId == c.id,
                        onTap: () => setState(() => _categoryId = c.id),
                      ),
                  ],
                ),
              ),
            ),
            loading: () => const SizedBox(height: 8),
            error: (e, _) => Text('$e'),
          ),
          // Danh sách món
          Expanded(
            child: menuAsync.when(
              data: (items) {
                if (items.isEmpty) {
                  return const EmptyState(
                    icon: CupertinoIcons.square_list,
                    title: 'Chưa có món',
                    subtitle: 'Thêm món ở tab Quản lý',
                  );
                }
                return ListView.separated(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 120),
                  itemCount: items.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 10),
                  itemBuilder: (_, i) {
                    final it = items[i];
                    final qty = _cart[it.id]?.quantity ?? 0;
                    return _MenuRow(item: it, qty: qty, onAdd: () => _add(it));
                  },
                );
              },
              loading: () =>
                  const Center(child: CupertinoActivityIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
        ],
      ),
      floatingBottom: _cart.isEmpty
          ? null
          : _CartBar(count: _count, total: _total, onTap: _openCart),
    );
  }
}

class _CategoryChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _CategoryChip(
      {required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: GestureDetector(
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
          decoration: BoxDecoration(
            color: selected ? AppColors.accent : AppColors.card,
            borderRadius: BorderRadius.circular(30),
            boxShadow: selected ? null : kCardShadow,
          ),
          child: Text(label,
              style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: selected ? Colors.white : AppColors.label)),
        ),
      ),
    );
  }
}

class _MenuRow extends StatelessWidget {
  final MenuItem item;
  final int qty;
  final VoidCallback onAdd;
  const _MenuRow({required this.item, required this.qty, required this.onAdd});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.name, style: AppText.headline),
                if (item.description != null &&
                    item.description!.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(item.description!,
                      style: AppText.footnote, maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                ],
                const SizedBox(height: 4),
                Text(formatVnd(item.price),
                    style: AppText.subhead.copyWith(
                        color: AppColors.accent, fontWeight: FontWeight.w700)),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Stack(
            clipBehavior: Clip.none,
            children: [
              Semantics(
                button: true,
                label: 'Thêm ${item.name}',
                child: GestureDetector(
                  onTap: onAdd,
                  child: Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: AppColors.accentSoft,
                      borderRadius: BorderRadius.circular(AppRadius.sm),
                    ),
                    child: const Icon(CupertinoIcons.plus,
                        color: AppColors.accent, size: 22),
                  ),
                ),
              ),
              if (qty > 0)
                Positioned(
                  right: -6,
                  top: -6,
                  child: Container(
                    padding: const EdgeInsets.all(5),
                    decoration: const BoxDecoration(
                        color: AppColors.accent, shape: BoxShape.circle),
                    constraints:
                        const BoxConstraints(minWidth: 20, minHeight: 20),
                    child: Text('$qty',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                            color: Colors.white,
                            fontSize: 11,
                            fontWeight: FontWeight.w700)),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _CartBar extends StatelessWidget {
  final int count;
  final int total;
  final VoidCallback onTap;
  const _CartBar(
      {required this.count, required this.total, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.transparent,
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 20),
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
          decoration: BoxDecoration(
            color: AppColors.accent,
            borderRadius: BorderRadius.circular(AppRadius.md),
            boxShadow: kCardShadow,
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(8)),
                child: Text('$count',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w700)),
              ),
              const SizedBox(width: 10),
              const Text('Xem giỏ hàng',
                  style: TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.w600)),
              const Spacer(),
              Text(formatVnd(total),
                  style: const TextStyle(
                      color: Colors.white,
                      fontSize: 17,
                      fontWeight: FontWeight.w700)),
            ],
          ),
        ),
      ),
    );
  }
}

class _CartSheet extends StatefulWidget {
  final Map<int, CartLine> cart;
  final String table;
  final int total;
  final bool submitting;
  final void Function(int itemId, int qty) onQty;
  final VoidCallback onSubmit;
  const _CartSheet({
    required this.cart,
    required this.table,
    required this.total,
    required this.submitting,
    required this.onQty,
    required this.onSubmit,
  });

  @override
  State<_CartSheet> createState() => _CartSheetState();
}

class _CartSheetState extends State<_CartSheet> {
  @override
  Widget build(BuildContext context) {
    final lines = widget.cart.values.toList();
    final total = lines.fold<int>(0, (s, l) => s + l.lineTotal);
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.6,
      maxChildSize: 0.9,
      minChildSize: 0.4,
      builder: (context, scroll) {
        return Column(
          children: [
            const SizedBox(height: 10),
            Container(
              width: 40,
              height: 5,
              decoration: BoxDecoration(
                  color: AppColors.separator,
                  borderRadius: BorderRadius.circular(3)),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 6),
              child: Row(
                children: [
                  Text('Giỏ hàng · Bàn ${widget.table}',
                      style: AppText.title),
                ],
              ),
            ),
            Expanded(
              child: lines.isEmpty
                  ? const EmptyState(
                      icon: CupertinoIcons.cart, title: 'Giỏ trống')
                  : ListView.separated(
                      controller: scroll,
                      padding: const EdgeInsets.fromLTRB(16, 6, 16, 16),
                      itemCount: lines.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (_, i) {
                        final l = lines[i];
                        return AppCard(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 14, vertical: 10),
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(l.item.name, style: AppText.headline),
                                    const SizedBox(height: 2),
                                    Text(formatVnd(l.item.price),
                                        style: AppText.footnote),
                                  ],
                                ),
                              ),
                              QtyStepper(
                                value: l.quantity,
                                onChanged: (q) {
                                  widget.onQty(l.item.id, q);
                                  setState(() {});
                                },
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 6, 16, 12),
                child: Column(
                  children: [
                    Row(
                      children: [
                        const Text('Tổng cộng', style: AppText.headline),
                        const Spacer(),
                        Text(formatVnd(total),
                            style: AppText.title
                                .copyWith(color: AppColors.accent)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    PrimaryButton(
                      label: 'Gửi đơn',
                      icon: CupertinoIcons.paperplane_fill,
                      loading: widget.submitting,
                      onPressed: lines.isEmpty ? null : widget.onSubmit,
                    ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
