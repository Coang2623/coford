import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repository.dart';
import '../providers.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/ui.dart';

const _methods = {
  'CASH': ('Tiền mặt', CupertinoIcons.money_dollar),
  'CARD': ('Thẻ', CupertinoIcons.creditcard),
  'TRANSFER': ('Chuyển khoản', CupertinoIcons.qrcode),
};

class PaymentPage extends ConsumerStatefulWidget {
  final int orderId;
  const PaymentPage({super.key, required this.orderId});

  @override
  ConsumerState<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends ConsumerState<PaymentPage> {
  String _method = 'CASH';
  bool _paying = false;

  Future<void> _pay() async {
    setState(() => _paying = true);
    try {
      await ref.read(repositoryProvider).payOrder(widget.orderId, _method);
      if (mounted) {
        setState(() => _paying = false);
        showToast(context, 'Thanh toán thành công');
      }
    } catch (e) {
      if (mounted) {
        setState(() => _paying = false);
        showToast(context, '$e', error: true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(orderProvider(widget.orderId));
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        scrolledUnderElevation: 0,
        leading: BackButton(color: AppColors.accent),
        title: const Text('Thanh toán', style: AppText.headline),
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
          final paid = data.order.status == 'PAID';
          return paid ? _invoice(context, ref, data) : _payForm(context, data);
        },
      ),
    );
  }

  // ----- Màn thanh toán (đơn NEW) -----
  Widget _payForm(BuildContext context, OrderWithItems data) {
    final settings = ref.watch(settingsProvider).value ?? {};
    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
            children: [
              AppCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    const Text('Số tiền cần thu', style: AppText.subhead),
                    const SizedBox(height: 6),
                    Text(formatVnd(data.order.totalAmount),
                        style: AppText.largeTitle
                            .copyWith(color: AppColors.accent)),
                    const SizedBox(height: 4),
                    Text('Bàn ${data.order.tableNo} · #${data.order.id}',
                        style: AppText.footnote),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              const SectionHeader('Phương thức'),
              Row(
                children: [
                  for (final e in _methods.entries) ...[
                    Expanded(
                      child: _MethodTile(
                        label: e.value.$1,
                        icon: e.value.$2,
                        selected: _method == e.key,
                        onTap: () => setState(() => _method = e.key),
                      ),
                    ),
                    if (e.key != _methods.keys.last) const SizedBox(width: 8),
                  ],
                ],
              ),
              if (_method == 'TRANSFER') ...[
                const SizedBox(height: 16),
                _TransferInfo(settings: settings, amount: data.order.totalAmount),
              ],
            ],
          ),
        ),
        SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
            child: PrimaryButton(
              label: 'Xác nhận thanh toán',
              icon: CupertinoIcons.checkmark_seal_fill,
              loading: _paying,
              onPressed: _pay,
            ),
          ),
        ),
      ],
    );
  }

  // ----- Hóa đơn (đơn PAID) -----
  Widget _invoice(BuildContext context, WidgetRef ref, OrderWithItems data) {
    final paymentFuture =
        ref.read(repositoryProvider).getPayment(widget.orderId);
    final settings = ref.watch(settingsProvider).value ?? {};
    final shopName = settings['shop_name'] ?? 'Coford Coffee';
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
      children: [
        AppCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            children: [
              const Icon(CupertinoIcons.checkmark_seal_fill,
                  color: AppColors.green, size: 44),
              const SizedBox(height: 8),
              Text(shopName, style: AppText.title),
              Text('HÓA ĐƠN THANH TOÁN', style: AppText.caption),
              const SizedBox(height: 14),
              const Divider(color: AppColors.separator),
              _kv('Bàn', data.order.tableNo),
              _kv('Mã đơn', '#${data.order.id}'),
              FutureBuilder(
                future: paymentFuture,
                builder: (_, snap) {
                  final p = snap.data;
                  return Column(
                    children: [
                      _kv('Phương thức',
                          p == null ? '—' : _methods[p.method]?.$1 ?? p.method),
                      _kv('Thời gian',
                          p == null ? '—' : formatDateTime(p.paidAt)),
                    ],
                  );
                },
              ),
              const Divider(color: AppColors.separator),
              const SizedBox(height: 4),
              for (final it in data.items)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 5),
                  child: Row(
                    children: [
                      Text('${it.quantity}×',
                          style: AppText.body
                              .copyWith(color: AppColors.accent)),
                      const SizedBox(width: 8),
                      Expanded(child: Text(it.itemName, style: AppText.body)),
                      Text(formatVnd(it.lineTotal), style: AppText.body),
                    ],
                  ),
                ),
              const Divider(color: AppColors.separator),
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Row(
                  children: [
                    const Text('TỔNG CỘNG', style: AppText.headline),
                    const Spacer(),
                    Text(formatVnd(data.order.totalAmount),
                        style:
                            AppText.title.copyWith(color: AppColors.accent)),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SecondaryButton(
          label: 'Xong',
          icon: CupertinoIcons.checkmark,
          onPressed: () => Navigator.of(context).maybePop(),
        ),
      ],
    );
  }

  Widget _kv(String k, String v) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Text(k, style: AppText.subhead),
          const Spacer(),
          Text(v, style: AppText.body.copyWith(fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

class _MethodTile extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;
  const _MethodTile(
      {required this.label,
      required this.icon,
      required this.selected,
      required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          color: selected ? AppColors.accent : AppColors.card,
          borderRadius: BorderRadius.circular(AppRadius.md),
          boxShadow: selected ? null : kCardShadow,
        ),
        child: Column(
          children: [
            Icon(icon,
                color: selected ? Colors.white : AppColors.accent, size: 26),
            const SizedBox(height: 6),
            Text(label,
                textAlign: TextAlign.center,
                style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: selected ? Colors.white : AppColors.label)),
          ],
        ),
      ),
    );
  }
}

/// Thông tin chuyển khoản tĩnh (QR placeholder + số tài khoản từ cấu hình).
class _TransferInfo extends StatelessWidget {
  final Map<String, String?> settings;
  final int amount;
  const _TransferInfo({required this.settings, required this.amount});

  @override
  Widget build(BuildContext context) {
    final bank = settings['bank_name'] ?? '—';
    final accNo = settings['bank_account_no'] ?? '—';
    final accName = settings['bank_account_name'] ?? '—';
    return AppCard(
      child: Column(
        children: [
          // QR placeholder (tĩnh, offline). Có thể thay bằng ảnh QR thật ở Quản lý.
          Container(
            width: 160,
            height: 160,
            decoration: BoxDecoration(
              color: AppColors.fill,
              borderRadius: BorderRadius.circular(AppRadius.md),
            ),
            child: const Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(CupertinoIcons.qrcode, size: 80, color: AppColors.label),
                SizedBox(height: 6),
                Text('Quét để chuyển khoản', style: AppText.caption),
              ],
            ),
          ),
          const SizedBox(height: 14),
          _row('Ngân hàng', bank),
          _row('Số tài khoản', accNo),
          _row('Chủ tài khoản', accName),
          _row('Số tiền', formatVnd(amount)),
        ],
      ),
    );
  }

  Widget _row(String k, String v) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            Text(k, style: AppText.subhead),
            const Spacer(),
            Text(v,
                style: AppText.body.copyWith(fontWeight: FontWeight.w600)),
          ],
        ),
      );
}
