export const FILTER_OPERATOR_LABELS: Record<string, string> = {
  EQ: "Equals",
  NE: "Not equals",
  LT: "Less than",
  LTE: "Less than or equal",
  GT: "Greater than",
  GTE: "Greater than or equal",
  BETWEEN: "Between",
  IN: "In list",
  IS_NULL: "Is null",
  LIKE: "Like",
  ILIKE: "Like (case insensitive)",
};

export function filterOperatorOptions(types: string[]) {
  return types.map((op) => ({ value: op, label: FILTER_OPERATOR_LABELS[op] ?? op }));
}
