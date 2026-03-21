class MetaModel {
  final int? id;
  final String? nome;
  final double valorAlvo;
  final double valorAtual;
  final String? prazo;
  final bool concluida;
  final double percentual;

  MetaModel({
    this.id,
    this.nome,
    required this.valorAlvo,
    required this.valorAtual,
    this.prazo,
    required this.concluida,
    required this.percentual,
  });

  factory MetaModel.fromJson(Map<String, dynamic> json) => MetaModel(
        id: json['id'] as int?,
        nome: json['nome'] as String?,
        valorAlvo: (json['valorAlvo'] as num).toDouble(),
        valorAtual: (json['valorAtual'] as num).toDouble(),
        prazo: json['prazo'] as String?,
        concluida: json['concluida'] as bool,
        percentual: (json['percentual'] as num).toDouble(),
      );
}
