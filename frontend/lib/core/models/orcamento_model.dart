class OrcamentoModel {
  final int? id;
  final int? categoriaId;
  final String? categoria;
  final double valorLimite;
  final int mes;
  final int ano;
  final double gasto;
  final double percentual;

  OrcamentoModel({
    this.id,
    this.categoriaId,
    this.categoria,
    required this.valorLimite,
    required this.mes,
    required this.ano,
    required this.gasto,
    required this.percentual,
  });

  factory OrcamentoModel.fromJson(Map<String, dynamic> json) => OrcamentoModel(
        id: json['id'] as int?,
        categoriaId: json['categoriaId'] as int?,
        categoria: json['categoria'] as String?,
        valorLimite: (json['valorLimite'] as num).toDouble(),
        mes: json['mes'] as int,
        ano: json['ano'] as int,
        gasto: (json['gasto'] as num).toDouble(),
        percentual: (json['percentual'] as num).toDouble(),
      );
}
