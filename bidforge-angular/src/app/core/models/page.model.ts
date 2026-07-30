export interface Page<T> {
  content: T[];

  totalElements: number;

  totalPages: number;

  number: number;

  size: number;

  numberOfElements: number;

  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface PageQuery {
  page?: number;
  size?: number;

  sort?: string;
}

export function emptyPage<T>(size = 12): Page<T> {
  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size,
    numberOfElements: 0,
    first: true,
    last: true,
    empty: true,
  };
}
