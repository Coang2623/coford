import 'package:intl/intl.dart';

final _vnd = NumberFormat.decimalPattern('vi_VN');
final _time = DateFormat('HH:mm');
final _dateTime = DateFormat('dd/MM/yyyy HH:mm');
final _date = DateFormat('dd/MM');

/// Định dạng tiền VND, ví dụ 35000 -> "35.000 đ"
String formatVnd(int amount) => '${_vnd.format(amount)} đ';

String formatTime(DateTime dt) => _time.format(dt.toLocal());

String formatDateTime(DateTime dt) => _dateTime.format(dt.toLocal());

String formatDateShort(DateTime dt) => _date.format(dt.toLocal());

/// "x phút trước" cho màn bếp
String relativeMinutes(DateTime from) {
  final mins = DateTime.now().difference(from).inMinutes;
  if (mins <= 0) return 'vừa xong';
  if (mins < 60) return '$mins phút trước';
  final h = mins ~/ 60;
  return '$h giờ ${mins % 60} phút trước';
}
