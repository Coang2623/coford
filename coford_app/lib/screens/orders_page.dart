import 'package:flutter/cupertino.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../app.dart';
import '../data/database.dart';
import '../providers.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/ui.dart';
import 'order_detail_page.dart';
import 'payment_page.dart';

class OrdersPage extends ConsumerStatefulWidget {
  const OrdersPage({super.key});

  @override
  ConsumerState<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends ConsumerState<OrdersPage> {
  String? _filter; // null = tất cả
  String _search = '';

  @override
  Widget build(BuildContext context) {
    final ordersAsync = ref.watch(ordersProvider(_filter));

    return IosScaffold(
      title: 'Đơn hàng',
      child: Column(
        children: [
          // Tìm kiếm
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
            child: CupertinoSearchTextField(
              placeholder: 'Tìm theo bàn hoặc mã đơn',
              onChanged: (v) => setState(() => _search = v.trim().toLowerCase()),
              backgroundColor: AppColors.fill,
            ),
          ),
          // Bộ lọc
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
            child: Segmented<String>(
              options: {
                'all': 'Tất cả',
                'NEW': 'Chờ TT',
                'PAID': 'Đã TT',
                'CANCELLED': 'Đã hủy',
              },
              value: _filter ?? 'all',
              onChanged: (v) =>
                  setState(() => _filter = v == 'all' ? null : v),
            ),
          ),
          Expanded(
            child: ordersAsync.when(
              data: (orders) {
                final filtered = _search.isEmpty
                    ? orders
                    : orders.where((o) {
                        return o.tableNo.toLowerCase().contains(_search) ||
                            '#${o.id}'.contains(_search) ||
                            '${o.id}'.contains(_search);
                      }).toList();
                if (filtered.isEmpty) {
                  return const EmptyState(
                    icon: CupertinoIcons.doc_text,
                    title: 'Không có đơn',
                    subtitle: 'Tạo đơn ở tab Bán hàng',
                  );
                }
                return ListView.separated(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
                  itemCount: filtered.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 10),
                  itemBuilder: (_, i) => _OrderCard(order: filtered[i]),
                );
              },
              loading: () => const Center(child: CupertinoActivityIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
        ],
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  final Order order;
  const _OrderCard({required this.order});

  void _open(BuildContext context) {
    Navigator.of(context).push(CupertinoPageRoute(
      builder: (_) => OrderDetailPage(orderId: order.id),
    ));
  }

  void _pay(BuildContext context) {
    Navigator.of(context).push(CupertinoPageRoute(
      builder: (_) => PaymentPage(orderId: order.id),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return AppCard(
      onTap: () => _open(context),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                    color: AppColors.accentSoft,
                    borderRadius: BorderRadius.circular(8)),
                child: Text(order.tableNo,
                    style: AppText.footnote.copyWith(
                        color: AppColors.accent, fontWeight: FontWeight.w700)),
              ),
              const SizedBox(width: 8),
              Text('#${order.id}', style: AppText.subhead),
              const Spacer(),
              StatusBadge(order.status),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(formatVnd(order.totalAmount),
                      style: AppText.title.copyWith(color: AppColors.accent)),
                  const SizedBox(height: 2),
                  Text(formatDateTime(order.createdAt),
                      style: AppText.caption),
                ],
              ),
              const Spacer(),
              if (order.status == 'NEW')
                PrimaryButton(
                  label: 'Thanh toán',
                  expand: false,
                  onPressed: () => _pay(context),
                )
              else
                SecondaryButton(
                  label: order.status == 'PAID' ? 'Hóa đơn' : 'Chi tiết',
                  onPressed: () => _open(context),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
