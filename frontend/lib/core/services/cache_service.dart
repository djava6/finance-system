import 'dart:convert';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import '../models/dashboard_model.dart';
import '../models/transaction_model.dart';

class CacheService {
  static Database? _db;

  static const _ttlTransacoes = Duration(minutes: 30);
  static const _ttlDashboard  = Duration(minutes: 15);

  Future<Database> get db async {
    _db ??= await _open();
    return _db!;
  }

  Future<Database> _open() async {
    final path = join(await getDatabasesPath(), 'finance_cache.db');
    return openDatabase(
      path,
      version: 1,
      onCreate: (db, _) async {
        await db.execute('''
          CREATE TABLE cache_transacoes (
            id INTEGER PRIMARY KEY,
            json TEXT NOT NULL,
            synced_at INTEGER NOT NULL
          )
        ''');
        await db.execute('''
          CREATE TABLE cache_kv (
            chave TEXT PRIMARY KEY,
            valor TEXT NOT NULL,
            updated_at INTEGER NOT NULL
          )
        ''');
      },
    );
  }

  // ── Transações ──────────────────────────────────────────────

  Future<void> saveTransacoes(List<TransactionModel> items) async {
    final d = await db;
    final now = DateTime.now().millisecondsSinceEpoch;
    final batch = d.batch();
    batch.delete('cache_transacoes');
    for (final t in items) {
      batch.insert('cache_transacoes', {
        'id': t.id,
        'json': jsonEncode(_transactionToJson(t)),
        'synced_at': now,
      });
    }
    await batch.commit(noResult: true);
  }

  Future<(List<TransactionModel>, DateTime?)> getTransacoes() async {
    final d = await db;
    final rows = await d.query('cache_transacoes', orderBy: 'id DESC');
    if (rows.isEmpty) return (<TransactionModel>[], null);
    final syncedAt = DateTime.fromMillisecondsSinceEpoch(
        rows.first['synced_at'] as int);
    if (DateTime.now().difference(syncedAt) > _ttlTransacoes) return (<TransactionModel>[], null);
    final items = rows
        .map((r) => TransactionModel.fromJson(
            jsonDecode(r['json'] as String) as Map<String, dynamic>))
        .toList();
    return (items, syncedAt);
  }

  // ── Dashboard ────────────────────────────────────────────────

  Future<void> saveDashboard(DashboardModel dashboard) async {
    await _saveKv('dashboard', jsonEncode(dashboard.toJson()), _ttlDashboard);
  }

  Future<(DashboardModel?, DateTime?)> getDashboard() async {
    final result = await _getKv('dashboard', _ttlDashboard);
    if (result == null) return (null, null);
    return (
      DashboardModel.fromJson(jsonDecode(result.$1) as Map<String, dynamic>),
      result.$2
    );
  }

  // ── Helpers KV ──────────────────────────────────────────────

  Future<void> _saveKv(String key, String value, Duration ttl) async {
    final d = await db;
    await d.insert(
      'cache_kv',
      {'chave': key, 'valor': value, 'updated_at': DateTime.now().millisecondsSinceEpoch},
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<(String, DateTime)?> _getKv(String key, Duration ttl) async {
    final d = await db;
    final rows = await d.query('cache_kv',
        where: 'chave = ?', whereArgs: [key]);
    if (rows.isEmpty) return null;
    final updatedAt = DateTime.fromMillisecondsSinceEpoch(
        rows.first['updated_at'] as int);
    if (DateTime.now().difference(updatedAt) > ttl) return null;
    return (rows.first['valor'] as String, updatedAt);
  }

  Future<void> clear() async {
    final d = await db;
    await d.delete('cache_transacoes');
    await d.delete('cache_kv');
  }

  // ── Serialização ─────────────────────────────────────────────

  Map<String, dynamic> _transactionToJson(TransactionModel t) => {
    'id': t.id,
    'descricao': t.descricao,
    'valor': t.valor,
    'tipo': t.tipo,
    'data': t.data.toIso8601String(),
    'categoriaId': t.categoriaId,
    'categoria': t.categoria,
    'contaId': t.contaId,
    'conta': t.conta,
    'recorrente': t.recorrente,
    'frequencia': t.frequencia,
    'proximaOcorrencia': t.proximaOcorrencia?.toIso8601String(),
    'reciboUrl': t.reciboUrl,
  };
}
