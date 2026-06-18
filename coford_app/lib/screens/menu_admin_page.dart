import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../app.dart';
import '../data/database.dart';
import '../providers.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/ui.dart';

class MenuAdminPage extends ConsumerStatefulWidget {
  const MenuAdminPage({super.key});

  @override
  ConsumerState<MenuAdminPage> createState() => _MenuAdminPageState();
}

class _MenuAdminPageState extends ConsumerState<MenuAdminPage> {
  int? _categoryId;

  Future<void> _addCategory() async {
    final ctrl = TextEditingController();
    final name = await showCupertinoDialog<String>(
      context: context,
      builder: (_) => CupertinoAlertDialog(
        title: const Text('Thêm danh mục'),
        content: Padding(
          padding: const EdgeInsets.only(top: 12),
          child: CupertinoTextField(
              controller: ctrl, placeholder: 'Tên danh mục', autofocus: true),
        ),
        actions: [
          CupertinoDialogAction(
              onPressed: () => Navigator.pop(context),
              child: const Text('Hủy')),
          CupertinoDialogAction(
              isDefaultAction: true,
              onPressed: () => Navigator.pop(context, ctrl.text.trim()),
              child: const Text('Thêm')),
        ],
      ),
    );
    if (name != null && name.isNotEmpty) {
      final cats = ref.read(categoriesProvider).value ?? [];
      await ref
          .read(repositoryProvider)
          .createCategory(name, cats.length + 1);
    }
  }

  Future<void> _editItem({MenuItem? item}) async {
    final cats = ref.read(categoriesProvider).value ?? [];
    if (cats.isEmpty) {
      showToast(context, 'Hãy thêm danh mục trước', error: true);
      return;
    }
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.background,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.lg)),
      ),
      builder: (_) => _ItemEditSheet(
          item: item, categories: cats, initialCategoryId: _categoryId),
    );
  }

  Future<void> _deleteItem(MenuItem item) async {
    final ok = await showCupertinoDialog<bool>(
      context: context,
      builder: (_) => CupertinoAlertDialog(
        title: Text('Xóa "${item.name}"?'),
        actions: [
          CupertinoDialogAction(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Không')),
          CupertinoDialogAction(
              isDestructiveAction: true,
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Xóa')),
        ],
      ),
    );
    if (ok == true) {
      await ref.read(repositoryProvider).deleteMenuItem(item.id);
      if (mounted) showToast(context, 'Đã xóa món');
    }
  }

  @override
  Widget build(BuildContext context) {
    final cats = ref.watch(categoriesProvider);
    final items =
        ref.watch(menuItemsProvider(MenuQuery(categoryId: _categoryId)));

    return IosScaffold(
      title: 'Quản lý',
      actions: [
        IconButton(
          icon: const Icon(CupertinoIcons.add_circled_solid,
              color: AppColors.accent),
          onPressed: () => _editItem(),
          tooltip: 'Thêm món',
        ),
      ],
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
        children: [
          // Filter danh mục
          cats.when(
            data: (list) => SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  _chip('Tất cả', _categoryId == null,
                      () => setState(() => _categoryId = null)),
                  for (final c in list)
                    _chip(c.name, _categoryId == c.id,
                        () => setState(() => _categoryId = c.id)),
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: GestureDetector(
                      onTap: _addCategory,
                      child: Container(
                        padding: const EdgeInsets.all(9),
                        decoration: BoxDecoration(
                            color: AppColors.card,
                            borderRadius: BorderRadius.circular(30),
                            boxShadow: kCardShadow),
                        child: const Icon(CupertinoIcons.add,
                            size: 18, color: AppColors.accent),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            loading: () => const SizedBox(height: 8),
            error: (e, _) => Text('$e'),
          ),
          const SizedBox(height: 12),
          // Danh sách món
          items.when(
            data: (list) {
              if (list.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.only(top: 40),
                  child: EmptyState(
                      icon: CupertinoIcons.square_list,
                      title: 'Chưa có món',
                      subtitle: 'Nhấn + để thêm món'),
                );
              }
              return Column(
                children: [
                  for (final it in list)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _AdminItemRow(
                        item: it,
                        onEdit: () => _editItem(item: it),
                        onDelete: () => _deleteItem(it),
                        onToggle: () => ref
                            .read(repositoryProvider)
                            .updateMenuItem(it.id, available: !it.available),
                      ),
                    ),
                ],
              );
            },
            loading: () => const Center(child: CupertinoActivityIndicator()),
            error: (e, _) => Text('$e'),
          ),
          const SizedBox(height: 8),
          const SectionHeader('Cấu hình quán & QR'),
          const _SettingsCard(),
        ],
      ),
    );
  }

  Widget _chip(String label, bool selected, VoidCallback onTap) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: GestureDetector(
        onTap: onTap,
        child: Container(
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

class _AdminItemRow extends StatelessWidget {
  final MenuItem item;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final VoidCallback onToggle;
  const _AdminItemRow({
    required this.item,
    required this.onEdit,
    required this.onDelete,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.fromLTRB(16, 10, 8, 10),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                        child: Text(item.name,
                            style: AppText.headline,
                            overflow: TextOverflow.ellipsis)),
                    if (!item.available) ...[
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                            color: AppColors.fill,
                            borderRadius: BorderRadius.circular(6)),
                        child: Text('Ngừng bán', style: AppText.caption),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 2),
                Text(formatVnd(item.price),
                    style: AppText.subhead.copyWith(color: AppColors.accent)),
              ],
            ),
          ),
          CupertinoSwitch(
            value: item.available,
            activeTrackColor: AppColors.green,
            onChanged: (_) => onToggle(),
          ),
          IconButton(
            icon: const Icon(CupertinoIcons.pencil,
                color: AppColors.blue, size: 20),
            onPressed: onEdit,
          ),
          IconButton(
            icon: const Icon(CupertinoIcons.trash,
                color: AppColors.red, size: 20),
            onPressed: onDelete,
          ),
        ],
      ),
    );
  }
}

/// Sheet thêm/sửa món.
class _ItemEditSheet extends ConsumerStatefulWidget {
  final MenuItem? item;
  final List<Category> categories;
  final int? initialCategoryId;
  const _ItemEditSheet(
      {this.item, required this.categories, this.initialCategoryId});

  @override
  ConsumerState<_ItemEditSheet> createState() => _ItemEditSheetState();
}

class _ItemEditSheetState extends ConsumerState<_ItemEditSheet> {
  late final TextEditingController _name =
      TextEditingController(text: widget.item?.name ?? '');
  late final TextEditingController _desc =
      TextEditingController(text: widget.item?.description ?? '');
  late final TextEditingController _price = TextEditingController(
      text: widget.item != null ? '${widget.item!.price}' : '');
  late int _categoryId = widget.item?.categoryId ??
      widget.initialCategoryId ??
      widget.categories.first.id;
  late bool _available = widget.item?.available ?? true;
  bool _saving = false;

  Future<void> _save() async {
    final name = _name.text.trim();
    final price = int.tryParse(_price.text.trim().replaceAll('.', ''));
    if (name.isEmpty || price == null || price < 0) {
      showToast(context, 'Nhập tên và giá hợp lệ', error: true);
      return;
    }
    setState(() => _saving = true);
    final repo = ref.read(repositoryProvider);
    try {
      if (widget.item == null) {
        await repo.createMenuItem(
          categoryId: _categoryId,
          name: name,
          description: _desc.text.trim().isEmpty ? null : _desc.text.trim(),
          price: price,
          available: _available,
        );
      } else {
        await repo.updateMenuItem(
          widget.item!.id,
          categoryId: _categoryId,
          name: name,
          description: _desc.text.trim(),
          price: price,
          available: _available,
        );
      }
      if (mounted) {
        Navigator.pop(context);
        showToast(context, 'Đã lưu món');
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
    return Padding(
      padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 14, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                    width: 40,
                    height: 5,
                    decoration: BoxDecoration(
                        color: AppColors.separator,
                        borderRadius: BorderRadius.circular(3))),
              ),
              const SizedBox(height: 16),
              Text(widget.item == null ? 'Thêm món' : 'Sửa món',
                  style: AppText.title),
              const SizedBox(height: 16),
              _label('Tên món'),
              CupertinoTextField(
                  controller: _name,
                  placeholder: 'VD: Cà phê sữa',
                  padding: const EdgeInsets.all(14),
                  decoration: _fieldDeco()),
              const SizedBox(height: 12),
              _label('Danh mục'),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14),
                decoration: _fieldDeco(),
                child: DropdownButton<int>(
                  value: _categoryId,
                  isExpanded: true,
                  underline: const SizedBox(),
                  items: [
                    for (final c in widget.categories)
                      DropdownMenuItem(value: c.id, child: Text(c.name)),
                  ],
                  onChanged: (v) => setState(() => _categoryId = v!),
                ),
              ),
              const SizedBox(height: 12),
              _label('Giá (đồng)'),
              CupertinoTextField(
                  controller: _price,
                  placeholder: 'VD: 25000',
                  keyboardType: TextInputType.number,
                  padding: const EdgeInsets.all(14),
                  decoration: _fieldDeco()),
              const SizedBox(height: 12),
              _label('Mô tả (tùy chọn)'),
              CupertinoTextField(
                  controller: _desc,
                  placeholder: 'VD: Béo ngậy',
                  padding: const EdgeInsets.all(14),
                  decoration: _fieldDeco()),
              const SizedBox(height: 16),
              Row(
                children: [
                  const Text('Đang bán', style: AppText.headline),
                  const Spacer(),
                  CupertinoSwitch(
                      value: _available,
                      activeTrackColor: AppColors.green,
                      onChanged: (v) => setState(() => _available = v)),
                ],
              ),
              const SizedBox(height: 20),
              PrimaryButton(
                  label: 'Lưu', loading: _saving, onPressed: _save),
            ],
          ),
        ),
      ),
    );
  }

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.only(bottom: 6, left: 2),
        child: Text(t, style: AppText.footnote),
      );

  BoxDecoration _fieldDeco() => BoxDecoration(
      color: AppColors.card,
      borderRadius: BorderRadius.circular(AppRadius.sm),
      border: Border.all(color: AppColors.separator));
}

/// Cấu hình quán + thông tin chuyển khoản.
class _SettingsCard extends ConsumerWidget {
  const _SettingsCard();

  Future<void> _edit(BuildContext context, WidgetRef ref, String key,
      String label, String? current) async {
    final ctrl = TextEditingController(text: current ?? '');
    final v = await showCupertinoDialog<String>(
      context: context,
      builder: (_) => CupertinoAlertDialog(
        title: Text(label),
        content: Padding(
          padding: const EdgeInsets.only(top: 12),
          child: CupertinoTextField(controller: ctrl, autofocus: true),
        ),
        actions: [
          CupertinoDialogAction(
              onPressed: () => Navigator.pop(context),
              child: const Text('Hủy')),
          CupertinoDialogAction(
              isDefaultAction: true,
              onPressed: () => Navigator.pop(context, ctrl.text.trim()),
              child: const Text('Lưu')),
        ],
      ),
    );
    if (v != null) {
      await ref.read(repositoryProvider).setSetting(key, v);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final s = ref.watch(settingsProvider).value ?? {};
    final rows = [
      ('shop_name', 'Tên quán', s['shop_name']),
      ('bank_name', 'Ngân hàng', s['bank_name']),
      ('bank_account_no', 'Số tài khoản', s['bank_account_no']),
      ('bank_account_name', 'Chủ tài khoản', s['bank_account_name']),
    ];
    return AppCard(
      padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 16),
      child: Column(
        children: [
          for (int i = 0; i < rows.length; i++) ...[
            if (i > 0) const Divider(height: 1, color: AppColors.separator),
            InkWell(
              onTap: () =>
                  _edit(context, ref, rows[i].$1, rows[i].$2, rows[i].$3),
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 14),
                child: Row(
                  children: [
                    Text(rows[i].$2, style: AppText.body),
                    const Spacer(),
                    Text(rows[i].$3 ?? '—',
                        style: AppText.subhead
                            .copyWith(color: AppColors.secondaryLabel)),
                    const SizedBox(width: 6),
                    const Icon(CupertinoIcons.chevron_forward,
                        size: 16, color: AppColors.tertiaryLabel),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
