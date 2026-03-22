import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/models/dashboard_model.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/dashboard_service.dart';
import '../../core/models/meta_model.dart';
import '../../core/models/orcamento_model.dart';
import '../../core/services/meta_service.dart';
import '../../core/services/orcamento_service.dart';
import '../accounts/account_list_screen.dart';
import '../budgets/budget_list_screen.dart';
import '../categories/category_list_screen.dart';
import '../goals/goal_list_screen.dart';
import '../profile/profile_screen.dart';
import '../transactions/transaction_list_screen.dart';

// ── Navigation shell ──────────────────────────────────────────────────────────

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  int _selectedIndex = 0;

  static const _tabs = [
    _HomeTab(),
    TransactionListScreen(),
    CategoryListScreen(),
    ProfileScreen(),
    AccountListScreen(),
    BudgetListScreen(),
    GoalListScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: _tabs,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (i) => setState(() => _selectedIndex = i),
        destinations: const [
          NavigationDestination(
              icon: Icon(Icons.home_outlined),
              selectedIcon: Icon(Icons.home),
              label: 'Início'),
          NavigationDestination(
              icon: Icon(Icons.list_alt_outlined),
              selectedIcon: Icon(Icons.list_alt),
              label: 'Transações'),
          NavigationDestination(
              icon: Icon(Icons.label_outlined),
              selectedIcon: Icon(Icons.label),
              label: 'Categorias'),
          NavigationDestination(
              icon: Icon(Icons.person_outline),
              selectedIcon: Icon(Icons.person),
              label: 'Perfil'),
          NavigationDestination(
              icon: Icon(Icons.account_balance_outlined),
              selectedIcon: Icon(Icons.account_balance),
              label: 'Contas'),
          NavigationDestination(
              icon: Icon(Icons.pie_chart_outline),
              selectedIcon: Icon(Icons.pie_chart),
              label: 'Orçamentos'),
          NavigationDestination(
              icon: Icon(Icons.flag_outlined),
              selectedIcon: Icon(Icons.flag),
              label: 'Metas'),
        ],
      ),
    );
  }
}

// ── Home tab (dashboard content) ─────────────────────────────────────────────

class _HomeTab extends StatefulWidget {
  const _HomeTab();

  @override
  State<_HomeTab> createState() => _HomeTabState();
}

class _HomeTabState extends State<_HomeTab> {
  final _service = DashboardService();
  final _orcamentoService = OrcamentoService();
  final _metaService = MetaService();

  DashboardModel? _data;
  List<OrcamentoModel> _orcamentosAlerta = [];
  List<MetaModel> _metasAndamento = [];
  bool _loading = true;
  bool _firstLoad = true;

  final _currency = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
  final _dateFormat = DateFormat('dd/MM');

  final _chartColors = [
    Colors.blue,
    Colors.orange,
    Colors.green,
    Colors.red,
    Colors.purple,
    Colors.teal,
    Colors.pink,
    Colors.amber,
    Colors.indigo,
  ];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final now = DateTime.now();
      final results = await Future.wait([
        _service.getDashboard(),
        _orcamentoService.listarPorMes(now.month, now.year).catchError((_) => <OrcamentoModel>[]),
        _metaService.listar().catchError((_) => <MetaModel>[]),
      ]);
      if (mounted) {
        _data = results[0] as DashboardModel;
        _orcamentosAlerta = (results[1] as List<OrcamentoModel>)
            .where((o) => o.percentual >= 80)
            .toList();
        _metasAndamento = (results[2] as List<MetaModel>)
            .where((m) => !m.concluida)
            .toList();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() { _loading = false; _firstLoad = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final nome = context.watch<AuthProvider>().nome ?? '';

    return Scaffold(
      appBar: AppBar(
        title: Text('Olá, $nome'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Sair',
            onPressed: () => context.read<AuthProvider>().logout(),
          ),
        ],
      ),
      body: _loading
          ? Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const CircularProgressIndicator(),
                  if (_firstLoad) ...[
                    const SizedBox(height: 16),
                    Text(
                      'Conectando ao servidor...',
                      style: Theme.of(context)
                          .textTheme
                          .bodyMedium
                          ?.copyWith(color: Colors.grey),
                    ),
                  ],
                ],
              ),
            )
          : _data == null
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text('Erro ao carregar dados.'),
                      const SizedBox(height: 12),
                      FilledButton.tonal(
                        onPressed: _load,
                        child: const Text('Tentar novamente'),
                      ),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  child: _buildContent(),
                ),
    );
  }

  Widget _buildContent() {
    final d = _data!;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // ── Cards de saldo ──────────────────────────
        _SaldoCard(
          saldo: d.saldo,
          totalReceitas: d.totalReceitas,
          totalDespesas: d.totalDespesas,
          currency: _currency,
        ),
        const SizedBox(height: 20),

        // ── Contas ───────────────────────────────────
        if (d.contas.isNotEmpty) ...[
          Text('Contas', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          Card(
            child: Column(
              children: d.contas.map((c) {
                return ListTile(
                  leading: const Icon(Icons.account_balance_outlined),
                  title: Text(c.nome),
                  trailing: Text(
                    _currency.format(c.saldo),
                    style: TextStyle(
                      color: c.saldo >= 0 ? Colors.green : Colors.red,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 20),
        ],

        // ── Orçamentos em alerta ─────────────────────
        if (_orcamentosAlerta.isNotEmpty) ...[
          Text('Orçamentos em alerta',
              style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          ..._orcamentosAlerta.map((o) {
            final pct = o.percentual.clamp(0.0, 100.0);
            final color = o.percentual >= 100 ? Colors.red : Colors.orange;
            return Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(o.categoria ?? 'Sem categoria',
                            style: const TextStyle(fontWeight: FontWeight.bold)),
                        Text('${pct.toStringAsFixed(0)}%',
                            style: TextStyle(color: color, fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 6),
                    LinearProgressIndicator(
                      value: pct / 100,
                      color: color,
                      backgroundColor: Colors.grey.shade200,
                      minHeight: 6,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${_currency.format(o.gasto)} / ${_currency.format(o.valorLimite)}',
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ],
                ),
              ),
            );
          }),
          const SizedBox(height: 20),
        ],

        // ── Metas em andamento ────────────────────────
        if (_metasAndamento.isNotEmpty) ...[
          Text('Metas em andamento',
              style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          ..._metasAndamento.map((m) {
            final pct = m.percentual.clamp(0.0, 100.0);
            return Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text(m.nome ?? '',
                              style: const TextStyle(fontWeight: FontWeight.bold)),
                        ),
                        Text('${pct.toStringAsFixed(0)}%',
                            style: TextStyle(
                                color: Theme.of(context).colorScheme.primary,
                                fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 6),
                    LinearProgressIndicator(
                      value: pct / 100,
                      minHeight: 6,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${_currency.format(m.valorAtual)} / ${_currency.format(m.valorAlvo)}',
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ],
                ),
              ),
            );
          }),
          const SizedBox(height: 20),
        ],

        // ── Gráfico de despesas por categoria ───────
        if (d.despesasPorCategoria.isNotEmpty) ...[
          Text('Despesas por categoria',
              style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          SizedBox(
            height: 200,
            child: PieChart(
              PieChartData(
                sections: d.despesasPorCategoria.asMap().entries.map((e) {
                  final color = _chartColors[e.key % _chartColors.length];
                  final pct = d.totalDespesas > 0
                      ? (e.value.total / d.totalDespesas * 100)
                      : 0.0;
                  return PieChartSectionData(
                    value: e.value.total,
                    title: '${pct.toStringAsFixed(0)}%',
                    color: color,
                    radius: 70,
                    titleStyle: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: Colors.white),
                  );
                }).toList(),
                sectionsSpace: 2,
                centerSpaceRadius: 32,
              ),
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 12,
            runSpacing: 4,
            children: d.despesasPorCategoria.asMap().entries.map((e) {
              final color = _chartColors[e.key % _chartColors.length];
              return Row(mainAxisSize: MainAxisSize.min, children: [
                Container(
                    width: 12,
                    height: 12,
                    decoration: BoxDecoration(
                        color: color, shape: BoxShape.circle)),
                const SizedBox(width: 4),
                Text(e.value.categoria,
                    style: const TextStyle(fontSize: 12)),
              ]);
            }).toList(),
          ),
          const SizedBox(height: 20),
        ],

        // ── Evolução mensal ──────────────────────────
        if (d.evolucaoMensal.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text('Evolução mensal',
              style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          SizedBox(
            height: 180,
            child: BarChart(
              BarChartData(
                barGroups: d.evolucaoMensal.asMap().entries.map((e) {
                  return BarChartGroupData(
                    x: e.key,
                    barRods: [
                      BarChartRodData(
                          toY: e.value.totalReceitas,
                          color: Colors.green.shade400,
                          width: 8),
                      BarChartRodData(
                          toY: e.value.totalDespesas,
                          color: Colors.red.shade400,
                          width: 8),
                    ],
                  );
                }).toList(),
                titlesData: FlTitlesData(
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      getTitlesWidget: (value, meta) {
                        final idx = value.toInt();
                        if (idx < 0 || idx >= d.evolucaoMensal.length) {
                          return const SizedBox();
                        }
                        final mes = d.evolucaoMensal[idx].mes;
                        return Text(
                            DateFormat.MMM('pt_BR')
                                .format(DateTime(2024, mes))
                                .substring(0, 3),
                            style: const TextStyle(fontSize: 10));
                      },
                    ),
                  ),
                  leftTitles:
                      const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  topTitles:
                      const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  rightTitles:
                      const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                borderData: FlBorderData(show: false),
                gridData: const FlGridData(show: false),
              ),
            ),
          ),
          const SizedBox(height: 8),
          Row(children: [
            _LegendDot(color: Colors.green.shade400, label: 'Receitas'),
            const SizedBox(width: 16),
            _LegendDot(color: Colors.red.shade400, label: 'Despesas'),
          ]),
          const SizedBox(height: 20),
        ],

        // ── Últimas transações ───────────────────────
        Text('Últimas transações',
            style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        ...d.ultimasTransacoes.map((t) => ListTile(
              contentPadding: EdgeInsets.zero,
              leading: CircleAvatar(
                backgroundColor: t.isReceita
                    ? Colors.green.shade100
                    : Colors.red.shade100,
                child: Icon(
                  t.isReceita ? Icons.arrow_upward : Icons.arrow_downward,
                  color: t.isReceita ? Colors.green : Colors.red,
                  size: 18,
                ),
              ),
              title: Text(t.descricao),
              subtitle: Text(_dateFormat.format(t.data)),
              trailing: Text(
                _currency.format(t.valor),
                style: TextStyle(
                  color: t.isReceita ? Colors.green : Colors.red,
                  fontWeight: FontWeight.bold,
                ),
              ),
            )),
      ],
    );
  }
}

// ── Shared display widgets ────────────────────────────────────────────────────

class _LegendDot extends StatelessWidget {
  final Color color;
  final String label;
  const _LegendDot({required this.color, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(mainAxisSize: MainAxisSize.min, children: [
      Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
      const SizedBox(width: 4),
      Text(label, style: const TextStyle(fontSize: 12)),
    ]);
  }
}

class _SaldoCard extends StatelessWidget {
  final double saldo;
  final double totalReceitas;
  final double totalDespesas;
  final NumberFormat currency;

  const _SaldoCard({
    required this.saldo,
    required this.totalReceitas,
    required this.totalDespesas,
    required this.currency,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            Text('Saldo atual',
                style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            Text(
              currency.format(saldo),
              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                    color: saldo >= 0 ? Colors.green : Colors.red,
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _InfoColumn(
                    label: 'Receitas',
                    value: currency.format(totalReceitas),
                    color: Colors.green),
                _InfoColumn(
                    label: 'Despesas',
                    value: currency.format(totalDespesas),
                    color: Colors.red),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoColumn extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _InfoColumn(
      {required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(label, style: TextStyle(color: color, fontSize: 13)),
        const SizedBox(height: 4),
        Text(value,
            style: TextStyle(
                color: color,
                fontWeight: FontWeight.bold,
                fontSize: 15)),
      ],
    );
  }
}
