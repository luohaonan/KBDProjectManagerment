import * as React from 'react';
import { cn } from '../../lib/utils';

export interface DataTableColumn<T> {
  header: string;
  accessor: keyof T | ((row: T) => React.ReactNode);
  searchable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
}

export interface DataTableProps<T> {
  data: T[];
  columns: DataTableColumn<T>[];
  rowsPerPageOptions?: number[];
  initialRowsPerPage?: number;
  className?: string;
}

export function DataTable<T>({
  data,
  columns,
  rowsPerPageOptions = [5, 10, 20],
  initialRowsPerPage = 5,
  className,
}: DataTableProps<T>) {
  const [searchText, setSearchText] = React.useState('');
  const [currentPage, setCurrentPage] = React.useState(1);
  const [rowsPerPage, setRowsPerPage] = React.useState(initialRowsPerPage);

  const searchableColumns = columns.filter(col => col.searchable !== false);

  const filteredData = React.useMemo(() => {
    if (!searchText.trim()) return data;
    const lowerSearch = searchText.toLowerCase();
    return data.filter(row => {
      return searchableColumns.some(col => {
        const value = typeof col.accessor === 'function' ? col.accessor(row) : row[col.accessor];
        return String(value).toLowerCase().includes(lowerSearch);
      });
    });
  }, [data, searchText, searchableColumns]);

  const pageCount = Math.max(1, Math.ceil(filteredData.length / rowsPerPage));
  const pageIndex = Math.min(currentPage, pageCount);
  const paginatedData = filteredData.slice((pageIndex - 1) * rowsPerPage, pageIndex * rowsPerPage);

  React.useEffect(() => {
    if (pageIndex !== currentPage) {
      setCurrentPage(pageIndex);
    }
  }, [pageCount, pageIndex, currentPage]);

  return (
    <div className={cn('space-y-4', className)}>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full sm:w-72">
          <input
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            placeholder="搜索"
            className="w-full rounded-md border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500"
          />
        </div>
        <div className="flex items-center gap-2 text-sm text-slate-300">
          <span>每页显示</span>
          <select
            value={rowsPerPage}
            onChange={e => setRowsPerPage(Number(e.target.value))}
            className="rounded-md border border-slate-700 bg-slate-800 px-2 py-1 text-slate-100"
          >
            {rowsPerPageOptions.map(option => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border border-slate-700">
        <table className="min-w-full divide-y divide-slate-700 bg-slate-900 text-sm">
          <thead className="bg-slate-800 text-slate-300">
            <tr>
              {columns.map((column, index) => (
                <th
                  key={index}
                  className={cn(
                    'px-4 py-3 text-left font-medium uppercase tracking-wide',
                    column.align === 'center' && 'text-center',
                    column.align === 'right' && 'text-right',
                    column.width
                  )}
                >
                  {column.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-700">
            {paginatedData.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-8 text-center text-slate-500">
                  未找到匹配记录。
                </td>
              </tr>
            ) : (
              paginatedData.map((row, rowIndex) => (
                <tr key={rowIndex} className="hover:bg-slate-800">
                  {columns.map((column, columnIndex) => {
                    const value = typeof column.accessor === 'function' ? column.accessor(row) : row[column.accessor];
                    return (
                      <td
                        key={columnIndex}
                        className={cn(
                          'px-4 py-3 align-top text-slate-100',
                          column.align === 'center' && 'text-center',
                          column.align === 'right' && 'text-right'
                        )}
                      >
                        {value != null ? (value as React.ReactNode) : '-'}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between text-slate-300 text-sm">
        <p>
          显示 {paginatedData.length} / {filteredData.length} 条记录
        </p>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
            disabled={pageIndex === 1}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1 transition hover:border-slate-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            上一页
          </button>
          <span>
            {pageIndex} / {pageCount}
          </span>
          <button
            type="button"
            onClick={() => setCurrentPage(prev => Math.min(prev + 1, pageCount))}
            disabled={pageIndex === pageCount}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1 transition hover:border-slate-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  );
}
