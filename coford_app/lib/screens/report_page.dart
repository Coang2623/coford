import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../app.dart';
import '../data/repository.dart';
import '../providers.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/ui.dart';

class ReportPage extends ConsumerStatefulWidget {
  const ReportPage({super.key});

  @override
  ConsumerState<ReportPage> createState() => _ReportPageState();
}

class _ReportPageState extends ConsumerState<ReportPage> {
  int _days = 7;

  @override
  Widget build(BuildContext context) {
    final revAsync = ref.watch(dailyRevenueProvider(_days));
    final topAsync = ref.watch(topItemsProvider(_days));

    return IosScaffold(
      title: 'Báo cáo',
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
        children: [
          Segmented<int>(
            options: const {7: '7 ngày', 30: '30 ngày'},
            value: _days,
            onChanged: (v) => setState(() => _days = v),
          ),
          const SizedBox(height: 16),
          revAsync.when(
            loading: () => const Padding(
              padding: EdgeInsets.all(40),
              child: Center(child: CupertinoActivityIndicator()),
            ),
            error: (e, _) => Text('$e'),
            data: (rev) => _buildRevenue(rev),
          ),
          const SizedBox(height: 16),
          const SectionHeader('Món bán chạy'),
          topAsync.when(
            loading: () => const SizedBox(),
            error: (e, _) => Text('$e'),
            data: (top) => _buildTop(top),
          ),
        ],
      ),
    );
  }

  Widget _buildRevenue(List<DailyRevenue> rev) {
    final totalRev = rev.fold<int>(0, (s, r) => s + r.revenue);
    final totalOrders = rev.fold<int>(0, (s, r) => s + r.orderCount);
    final avg = totalOrders == 0 ? 0 : totalRev ~/ totalOrders;
    final maxRev =
        rev.fold<int>(1, (m, r) => r.revenue > m ? r.revenue : m);

    return Column(
      children: [
        Row(
          children: [
            Expanded(
                child: _StatCard(
                    label: 'Doanh thu',
                    value: formatVnd(totalRev),
                    icon: CupertinoIcons.money_dollar_circle_fill,
                    color: AppColors.green)),
            const SizedBox(width: 12),
            Expanded(
                child: _StatCard(
                    label: 'Số đơn',
                    value: '$totalOrders',
                    icon: CupertinoIcons.doc_text_fill,
                    color: AppColors.blue)),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
                child: _StatCard(
                    label: 'TB / đơn',
                    value: formatVnd(avg),
                    icon: CupertinoIcons.chart_pie_fill,
                    color: AppColors.orange)),
            const SizedBox(width: 12),
            Expanded(
                child: _StatCard(
                    label: 'Ngày có DT',
                    value: '${rev.where((r) => r.revenue > 0).length}',
                    icon: CupertinoIcons.calendar,
                    color: AppColors.accent)),
          ],
        ),
        const SizedBox(height: 16),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Doanh thu theo ngày', style: AppText.headline),
              const SizedBox(height: 16),
              SizedBox(
                height: 150,
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    for (final r in rev)
                      Expanded(
                        child: Padding(
                          padding:
                              const EdgeInsets.symmetric(horizontal: 2),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              Container(
                                height: (r.revenue / maxRev * 120)
                                    .clamp(2, 120)
                                    .toDouble(),
                                decoration: BoxDecoration(
                                  color: r.revenue > 0
                                      ? AppColors.accent
                                      : AppColors.separator,
                                  borderRadius:
                                      BorderRadius.circular(4),
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(formatDateShort(r.date),
                                  style: const TextStyle(
                                      fontSize: 8,
                                      color: AppColors.tertiaryLabel)),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildTop(List<TopItem> top) {
    if (top.isEmpty) {
      return const AppCard(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 12),
          child: Center(
              child: Text('Chưa có dữ liệu bán hàng',
                  style: AppText.subhead)),
        ),
      );
    }
    final maxQty = top.first.totalQuantity;
    return AppCard(
      padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 16),
      child: Column(
        children: [
          for (int i = 0; i < top.length; i++) ...[
            if (i > 0) const Divider(height: 1, color: AppColors.separator),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 12),
              child: Row(
                children: [
                  Container(
                    width: 26,
                    height: 26,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                        color: AppColors.accentSoft,
                        borderRadius: BorderRadius.circular(8)),
                    child: Text('${i + 1}',
                        style: AppText.footnote.copyWith(
                            color: AppColors.accent,
                            fontWeight: FontWeight.w700)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(top[i].name, style: AppText.body),
                        const SizedBox(height: 6),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(3),
                          child: LinearProgressIndicator(
                            value: top[i].totalQuantity / maxQty,
                            minHeight: 6,
                            backgroundColor: AppColors.fill,
                            color: AppColors.accent,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text('${top[i].totalQuantity} ly',
                          style: AppText.body
                              .copyWith(fontWeight: FontWeight.w700)),
                      Text(formatVnd(top[i].totalAmount),
                          style: AppText.caption),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final Color color;
  const _StatCard(
      {required this.label,
      required this.value,
      required this.icon,
      required this.color});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(height: 10),
          Text(value,
              style: AppText.title.copyWith(fontSize: 20),
              maxLines: 1,
              overflow: TextOverflow.ellipsis),
          const SizedBox(height: 2),
          Text(label, style: AppText.footnote),
        ],
      ),
    );
  }
}
