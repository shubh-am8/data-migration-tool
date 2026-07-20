export function postLoginDestination(me: { authenticated: boolean }): "/dashboard" | null {
  return me.authenticated ? "/dashboard" : null;
}
