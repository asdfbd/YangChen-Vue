import {requestClient} from '#/api/request';

export interface ServerInfo {
  cpu?: Record<string, any>;
  jvm?: Record<string, any>;
  mem?: Record<string, any>;
  sys?: Record<string, any>;
  sysFiles?: Array<Record<string, any>>;
}

export const getServerApi = () => requestClient.get<ServerInfo>('/monitor/server');
