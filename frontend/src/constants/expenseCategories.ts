export interface ExpenseCategoryOption {
  value: string;
  label: string;
  backendCategory: string;
  description?: string;
}

export const EXPENSE_CATEGORY_OPTIONS: ExpenseCategoryOption[] = [
  {
    value: 'INTERNAL',
    label: '内部研发费用',
    backendCategory: 'INTERNAL',
    description: '包括：人力、实验耗材、设备折旧等',
  },
  {
    value: 'EXTERNAL',
    label: '外部合作费用',
    backendCategory: 'EXTERNAL',
    description: '包括：CRO/CDMO、临床中心、第三方服务等',
  },
  {
    value: 'EQUIPMENT',
    label: '设备采购',
    backendCategory: 'INTERNAL',
    description: '包括：研发设备、实验仪器、配套设施采购等',
  },
  {
    value: 'TRAVEL',
    label: '差旅费',
    backendCategory: 'INTERNAL',
    description: '包括：项目调研、会议、出差交通与住宿等',
  },
  {
    value: 'CONSULTING',
    label: '咨询费',
    backendCategory: 'EXTERNAL',
    description: '包括：顾问服务、外部专家、专项咨询等',
  },
  {
    value: 'OTHER',
    label: '其他',
    backendCategory: 'EXTERNAL',
    description: '包括：暂不归属前述分类的其他支出',
  },
];

export const EXPENSE_CATEGORY_LABEL_MAP = Object.fromEntries(
  EXPENSE_CATEGORY_OPTIONS.map((item) => [item.value, item.label])
);
