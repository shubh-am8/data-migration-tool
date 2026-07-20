export interface HttpStatusCounts {
  "2xx": number;
  "4xx": number;
  "5xx": number;
}

export interface HttpRouteStat {
  method: string;
  uri: string;
  count: number;
  meanMs: number;
  maxMs: number;
}

export interface HttpSnapshot {
  status: HttpStatusCounts;
  routes: HttpRouteStat[];
  slowest: HttpRouteStat[];
  fastest: HttpRouteStat[];
  choking: HttpRouteStat[];
}
