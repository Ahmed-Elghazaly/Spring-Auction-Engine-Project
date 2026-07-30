import { AuditAction } from './enums';

export interface AuditEventResponse {
  id: number;

  actor: string;
  action: AuditAction;

  entityType: string;

  entityId: number;

  details: string | null;
  createdAt: string;
}

export interface AuditFilters {
  entityType?: string;
  entityId?: number;
  actor?: string;
}
