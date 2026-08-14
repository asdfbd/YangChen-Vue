import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

export interface SysJob {
  concurrent?: string;
  createTime?: string;
  cronExpression?: string;
  invokeTarget?: string;
  jobGroup?: string;
  jobId?: number | string;
  jobName?: string;
  misfirePolicy?: string;
  remark?: string;
  status?: string;
}

export const listJobApi = (params: Recordable<any>) =>
  requestClient.get<{ rows: SysJob[]; total: number }>('/monitor/job/list', {params});
export const getJobApi = (jobId: number | string) => requestClient.get<SysJob>(`/monitor/job/${jobId}`);
export const addJobApi = (data: SysJob) => requestClient.post('/monitor/job', data);
export const updateJobApi = (data: SysJob) => requestClient.put('/monitor/job', data);
export const deleteJobApi = (jobIds: Array<number | string>) => requestClient.delete(`/monitor/job/${jobIds.join(',')}`);
export const changeJobStatusApi = (jobId: number | string, status: string) =>
  requestClient.put('/monitor/job/changeStatus', {jobId, status});
export const runJobApi = (jobId: number | string, jobGroup?: string) =>
  requestClient.put('/monitor/job/run', {jobGroup, jobId});
