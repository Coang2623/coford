import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../app.dart';
import '../data/database.dart';
import '../providers.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/ui.dart';

class KitchenPage extends ConsumerWidget {
  const KitchenPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(kitchenProvider);
    return IosScaffold(
      title: 'Bếp',
      actions: [
        async.when(
          data: (orders) => Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
                color: AppColors.accentSoft,
                borderRadius: BorderRadius.circular(20)),
            child: Text('${orders.length} đơn',
                style: AppText.footnote.copyWith(
                    color: AppColors.accent, fontWeight: FontWeight.w700)),
          ),
          loading: () => const SizedBox(),
          error: (_, __) => const SizedBox(),
        ),
        const SizedBox(width: 12),
      ],
      child: async.when(
        loading: () => const Center(child: CupertinoActivityIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (orders) {
          if (orders.isEmpty) {
            return const EmptyState(
              icon: CupertinoIcons.checkmark_circle,
              title: 'Hết đơn cần pha',
              subtitle: 'Đơn mới sẽ tự hiện ở đây',
            );
          }
          return LayoutBuilder(builder: (context, c) {
            final cols = c.maxWidth > 900
                ? 3
                : c.maxWidth > 600
                    ? 2
                    : 1;
            return GridView.builder(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: cols,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: 1.05,
              ),
              itemCount: orders.length,
              itemBuilder: (_, i) => _KitchenCard(order: orders[i]),
            );
          });
        },
      ),
    );
  }
}

class _KitchenCard extends ConsumerWidget {
  final Order order;
  const _KitchenCard({required this.order});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mins = DateTime.now().difference(order.createdAt).inMinutes;
    final urgent = mins >= 5;
    final detail = ref.watch(orderProvider(order.id));

    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(AppRadius.md),
        boxShadow: kCardShadow,
        border: urgent
            ? Border.all(color: AppColors.red.withValues(alpha: 0.5), width: 1.5)
            : null,
      ),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                    color: urgent ? AppColors.red : AppColors.accent,
                    borderRadius: BorderRadius.circular(8)),
                child: Text(order.tableNo,
                    style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                        fontSize: 14)),
              ),
              const SizedBox(width: 8),
              Text('#${order.id}', style: AppText.footnote),
              const Spacer(),
              Icon(CupertinoIcons.time,
                  size: 14,
                  color: urgent ? AppColors.red : AppColors.tertiaryLabel),
              const SizedBox(width: 3),
              Text(relativeMinutes(order.createdAt),
                  style: AppText.caption.copyWith(
                      color: urgent ? AppColors.red : AppColors.tertiaryLabel,
                      fontWeight: FontWeight.w600)),
            ],
          ),
          const SizedBox(height: 10),
          Expanded(
            child: detail.when(
              data: (d) => ListView(
                padding: EdgeInsets.zero,
                children: [
                  for (final it in d?.items ?? [])
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${it.quantity}×',
                              style: AppText.headline
                                  .copyWith(color: AppColors.accent)),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(it.itemName, style: AppText.body),
                                if (it.note != null && it.note!.isNotEmpty)
                                  Text(it.note!,
                                      style: AppText.footnote.copyWith(
                                          color: AppColors.orange)),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              loading: () => const SizedBox(),
              error: (e, _) => Text('$e'),
            ),
          ),
          const SizedBox(height: 8),
          PrimaryButton(
            label: 'Hoàn thành',
            icon: CupertinoIcons.checkmark_alt,
            color: AppColors.green,
            onPressed: () =>
                ref.read(repositoryProvider).setPrepared(order.id, true),
          ),
        ],
      ),
    );
  }
}
