export type Tipo = "RECEITA" | "DESPESA";
export interface Perfil {
  id: string;
  nome: string;
  email: string;
  papel: "TITULAR" | "ADMIN" | "DPO";
  emailVerificado: boolean;
}
export interface Categoria {
  id: string;
  usuarioId: string | null;
  nome: string;
  tipo: Tipo;
  cor: string;
  ativa: boolean;
}
export interface Lancamento {
  id: string;
  tipo: Tipo;
  valor: string;
  data: string;
  categoriaId: string;
  descricao: string;
  estabelecimento: string;
  formaPagamento: string;
  origem: string;
  excluidoEm: string | null;
}
export interface Entrada {
  tipo: Tipo;
  valor: string;
  data: string;
  categoriaId: string;
  descricao: string;
  estabelecimento: string;
  formaPagamento: string;
}
export interface Totais {
  receitas: string;
  despesas: string;
  saldo: string;
}
export interface Pagina {
  itens: Lancamento[];
  pagina: number;
  paginas: number;
  quantidade: number;
  totais: Totais;
}
export interface Painel {
  totais: Totais;
  distribuicao: {
    categoriaId: string;
    categoria: string;
    cor: string;
    valor: string;
    percentual: string;
  }[];
  serie: { mes: string; receitas: string; despesas: string }[];
  projecaoDespesas: string;
  mesProjecao: string;
}
export interface Meta {
  id: string;
  categoriaId: string;
  categoria: string;
  cor: string;
  mes: string;
  limite: string;
  consumido: string;
  percentual: string;
  estado: string;
}
export interface Sugestao {
  id: string | null;
  campos: Partial<Entrada>;
  confianca: Record<string, string>;
  pendentes: string[];
  conferirValor: boolean;
  manual: boolean;
  aviso: string;
}
export interface Diagnostico {
  texto: string;
  aviso: string;
  variacoes: {
    categoria: string;
    anterior: string;
    atual: string;
    diferenca: string;
  }[];
}
export function hoje(): string {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Bahia" }).format(
    new Date(),
  );
}
export function limitesMes(mes: string): { inicio: string; fim: string } {
  const [ano, numero] = mes.split("-").map(Number);
  return {
    inicio: mes + "-01",
    fim: mes + "-" + new Date(ano, numero, 0).getDate(),
  };
}
