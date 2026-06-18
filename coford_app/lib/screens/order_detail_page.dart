import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/database.dart';
import '../data/repository.dart';
import '../providers.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/ui.dart';
import 'payment_page.dart';

class OrderDetailPage extends ConsumerWidget {
  final int orderId;
  const OrderDetailPage({super.key, required this.orderId});

  Future<void> _cancel(BuildContext context, WidgetRef ref) async {
    final ok = await showCupertinoDialog<bool>(
      context: context,
      builder: (_) => CupertinoAlertDialog(
        title: const Text('Hủy đơn?'),
        content: const Text('Đơn sẽ chuyển sang trạng thái Đã hủy.'),
        actions: [
          CupertinoDialogAction(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Không')),
          CupertinoDialogAction(
              isDestructiveAction: true,
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Hủy đơn')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(repositoryProvider).cancelOrder(orderId);
      if (context.mounted) showToast(context, 'Đã hủy đơn');
    } catch (e) {
      if (context.mounted) showToast(context, '$e', error: true);
    }
  }

  Future<void> _edit(
      BuildContext context, WidgetRef ref, OrderWithItems data) async {
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.background,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.lg)),
      ),
      builder: (_) => _EditItemsSheet(orderId: orderId, current: data.items),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(orderProvider(orderId));
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        scrolledUnderElevation: 0,
        leading: const BackButton(color: AppColors.accent),
        title: Text('Đơn #$orderId', style: AppText.headline),
        centerTitle: false,
      ),
      body: async.when(
        loading: () => const Center(child: CupertinoActivityIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (data) {
          if (data == null) {
            return const EmptyState(
                icon: CupertinoIcons.exclamationmark_circle,
                title: 'Không tìm thấy đơn');
          }
          final order = data.order;
          final isNew = order.status == 'NEW';
          return Column(
            children: [
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
                  children: [
                    AppCard(
                      child: Row(
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Bàn ${order.tableNo}',
                                  style: AppText.title),
                              const SizedBox(height: 4),
                              Text(formatDateTime(order.createdAt),
                                  style: AppText.footnote),
                            ],
                          ),
                          const Spacer(),
                          StatusBadge(order.status),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                    const SectionHeader('Các món'),
                    AppCard(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Column(
                        children: [
                          for (int i = 0; i < data.items.length; i++) ...[
                            if (i > 0)
                              const Divider(
                                  height: 1, color: AppColors.separator),
                            _ItemRow(item: data.items[i]),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                    AppCard(
                      child: Row(
                        children: [
                          const Text('Tổng cộng', style: AppText.headline),
                          const Spacer(),
                          Text(formatVnd(order.totalAmount),
                              style: AppText.title
                                  .copyWith(color: AppColors.accent)),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              SafeArea(
                top: false,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                  child: Row(
                    children: [
                      if (isNew) ...[
                        SecondaryButton(
                          label: 'Sửa',
                          icon: CupertinoIcons.pencil,
                          onPressed: () => _edit(context, ref, data),
                        ),
                        const SizedBox(width: 8),
                        SecondaryButton(
                          label: 'Hủy',
                          icon: CupertinoIcons.trash,
                          tint: AppColors.red,
                          onPressed: () => _cancel(context, ref),
                        ),
                        const Spacer(),
                        PrimaryButton(
                          label: 'Thanh toán',
                          expand: false,
                          onPressed: () => Navigator.of(context).push(
                            CupertinoPageRoute(
                                builder: (_) =>
                                    PaymentPage(orderId: orderId)),
                          ),
                        ),
                      ] else
                        Expanded(
                          child: PrimaryButton(
                            label: order.status == 'PAID'
                                ? 'Xem hóa đơn'
                                : 'Đơn đã hủy',
                            onPressed: order.status == 'PAID'
                                ? () => Navigator.of(context).push(
                                      CupertinoPageRoute(
                                          builder: (_) =>
                                              PaymentPage(orderId: orderId)),
                                    )
                                : null,
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _ItemRow extends StatelessWidget {
  final OrderItem item;
  const _ItemRow({required this.item});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          Container(
            width: 28,
            alignment: Alignment.center,
            child: Text('${item.quantity}×',
                style: AppText.headline.copyWith(color: AppColors.accent)),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.itemName, style: AppText.body),
                if (item.note != null && item.note!.isNotEmpty)
                  Text(item.note!, style: AppText.footnote),
              ],
            ),
          ),
          Text(formatVnd(item.lineTotal),
              style: AppText.body.copyWith(fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

/// Sheet sửa danh sách món: chọn từ toàn menu, chỉnh số lượng.
class _EditItemsSheet extends ConsumerStatefulWidget {
  final int orderId;
  final List<OrderItem> current;
  const _EditItemsSheet({required this.orderId, required this.current});

  @override
  ConsumerState<_EditItemsSheet> createState() => _EditItemsSheetState();
}

class _EditItemsSheetState extends ConsumerState<_EditItemsSheet> {
  // menuItemId -> quantity
  late final Map<int, int> _qty = {
    for (final i in widget.current) i.menuItemId: i.quantity
  };
  bool _saving = false;

  Future<void> _save(List<MenuItem> menu) async {
    final lines = <CartLine>[];
    for (final m in menu) {
      final q = _qty[m.id] ?? 0;
      if (q > 0) lines.add(CartLine(m, quantity: q));
    }
    if (lines.isEmpty) {
      showToast(context, 'Đơn phải có ít nhất 1 món', error: true);
      return;
    }
    setState(() => _saving = true);
    try {
      await ref.read(repositoryProvider).updateOrderItems(widget.orderId, lines);
      if (mounted) {
        Navigator.pop(context);
        showToast(context, 'Đã cập nhật đơn');
      }
    } catch (e) {
      if (mounted) {
        setState(() => _saving = false);
        showToast(context, '$e', error: true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final menuAsync =
        ref.watch(menuItemsProvider(const MenuQuery(onlyAvailable: true)));
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.75,
      maxChildSize: 0.92,
      builder: (context, scroll) {
        return Column(
          children: [
            const SizedBox(height: 10),
            Container(
                width: 40,
                height: 5,
                decoration: BoxDecoration(
                    color: AppColors.separator,
                    borderRadius: BorderRadius.circular(3))),
            const Padding(
              padding: EdgeInsets.fromLTRB(20, 14, 20, 8),
              child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Sửa món', style: AppText.title)),
            ),
            Expanded(
              child: menuAsync.when(
                loading: () =>
                    const Center(child: CupertinoActivityIndicator()),
                error: (e, _) => Center(child: Text('$e')),
                data: (menu) => ListView.separated(
                  controller: scroll,
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
                  itemCount: menu.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 8),
                  itemBuilder: (_, i) {
                    final m = menu[i];
                    return AppCard(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 14, vertical: 10),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(m.name, style: AppText.headline),
                                Text(formatVnd(m.price),
                                    style: AppText.footnote),
                              ],
                            ),
                          ),
                          QtyStepper(
                            value: _qty[m.id] ?? 0,
                            onChanged: (q) => setState(() {
                              if (q <= 0) {
                                _qty.remove(m.id);
                              } else {
                                _qty[m.id] = q;
                              }
                            }),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
            ),
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 6, 16, 12),
                child: PrimaryButton(
                  label: 'Lưu thay đổi',
                  loading: _saving,
                  onPressed: () {
                    final menu = menuAsync.value ?? [];
                    _save(menu);
                  },
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
