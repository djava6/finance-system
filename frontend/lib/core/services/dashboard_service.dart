import 'dart:convert';
import 'dart:io';
import '../constants/api_constants.dart';
import '../models/dashboard_model.dart';
import 'api_client.dart';
import 'cache_service.dart';

class DashboardService {
  final _client = ApiClient();
  final _cache = CacheService();

  /// Returns (dashboard, isOffline, cachedAt)
  Future<(DashboardModel, bool, DateTime?)> getDashboard() async {
    try {
      final response = await _client.get(Uri.parse(ApiConstants.dashboard));
      if (response.statusCode == 200) {
        final model = DashboardModel.fromJson(
            jsonDecode(response.body) as Map<String, dynamic>);
        await _cache.saveDashboard(model);
        return (model, false, null);
      }
      throw Exception('Erro ao carregar dashboard (HTTP ${response.statusCode})');
    } on SocketException {
      final (cached, cachedAt) = await _cache.getDashboard();
      if (cached != null) return (cached, true, cachedAt);
      rethrow;
    }
  }
}
