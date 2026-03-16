import '../models/conta_model.dart';
import '../models/transaction_model.dart';

class EvolucaoMensalModel {
  final int ano;
  final int mes;
  final double totalReceitas;
  final double totalDespesas;

  EvolucaoMensalModel({
    required this.ano,
    required this.mes,
    required this.totalReceitas,
    required this.totalDespesas,
  });

  factory EvolucaoMensalModel.fromJson(Map<String, dynamic> json) =>
      EvolucaoMensalModel(
        ano: json['ano'] as int,
        mes: json['mes'] as int,
        totalReceitas: (json['totalReceitas'] as num).toDouble(),
        totalDespesas: (json['totalDespesas'] as num).toDouble(),
      );
}

class DespesaPorCategoriaModel {
  final String categoria;
  final double total;

  DespesaPorCategoriaModel({required this.categoria, required this.total});

  factory DespesaPorCategoriaModel.fromJson(Map<String, dynamic> json) =>
      DespesaPorCategoriaModel(
        categoria: json['categoria'] as String,
        total: (json['total'] as num).toDouble(),
      );
}

class DashboardModel {
  final double saldo;
  final double totalReceitas;
  final double totalDespesas;
  final List<ContaModel> contas;
  final List<TransactionModel> ultimasTransacoes;
  final List<DespesaPorCategoriaModel> despesasPorCategoria;
  final List<EvolucaoMensalModel> evolucaoMensal;

  DashboardModel({
    required this.saldo,
    required this.totalReceitas,
    required this.totalDespesas,
    required this.contas,
    required this.ultimasTransacoes,
    required this.despesasPorCategoria,
    required this.evolucaoMensal,
  });

  factory DashboardModel.fromJson(Map<String, dynamic> json) {
    return DashboardModel(
      saldo: (json['saldo'] as num).toDouble(),
      totalReceitas: (json['totalReceitas'] as num).toDouble(),
      totalDespesas: (json['totalDespesas'] as num).toDouble(),
      contas: (json['contas'] as List? ?? [])
          .map((e) => ContaModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      ultimasTransacoes: (json['ultimasTransacoes'] as List? ?? [])
          .map((e) => TransactionModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      despesasPorCategoria: (json['despesasPorCategoria'] as List? ?? [])
          .map((e) => DespesaPorCategoriaModel.fromJson(e))
          .toList(),
      evolucaoMensal: (json['evolucaoMensal'] as List? ?? [])
          .map((e) => EvolucaoMensalModel.fromJson(e))
          .toList(),
    );
  }
}
