export type HttpStatusFilter = "all" | "2xx" | "4xx" | "5xx";

export type HttpStatusSeriesKey = "http2xx" | "http4xx" | "http5xx";

export function seriesKeys(filter: HttpStatusFilter): HttpStatusSeriesKey[] {
  if (filter === "all") return ["http2xx", "http4xx", "http5xx"];
  return [`http${filter}` as HttpStatusSeriesKey];
}
