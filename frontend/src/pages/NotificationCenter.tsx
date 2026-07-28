import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/api';
import { Button } from '../components/ui/button';

interface NotificationItem {
  id: number;
  recipientUserId: number;
  type: string;
  title: string;
  content: string;
  projectId: number | null;
  milestoneCode: string | null;
  relatedUserId: number | null;
  relatedUserName: string | null;
  isRead: boolean;
  createdAt: string;
}

const PAGE_SIZE = 20;

const typeLabels: Record<string, string> = {
  PROJECT_COMPLETION: '项目完善',
  DELIVERABLE_UPLOADED: '交付物上传',
  REVIEW_SUBMITTED: '评审提交',
  REVIEW_DECIDED: '评审结果',
  MILESTONE_APPROVED: '里程碑通过',
  REVIEW_REJECTED: '评审退回',
};

const typeColors: Record<string, string> = {
  PROJECT_COMPLETION: 'bg-blue-100 text-blue-800',
  DELIVERABLE_UPLOADED: 'bg-green-100 text-green-800',
  REVIEW_SUBMITTED: 'bg-purple-100 text-purple-800',
  REVIEW_DECIDED: 'bg-orange-100 text-orange-800',
  MILESTONE_APPROVED: 'bg-teal-100 text-teal-800',
  REVIEW_REJECTED: 'bg-red-100 text-red-800',
};

const NotificationCenter: React.FC = () => {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  const fetchNotifications = useCallback(async (pageNum: number) => {
    setLoading(true);
    try {
      const res = await api.get('/api/notifications', {
        params: { page: pageNum, size: PAGE_SIZE },
      });
      if (res.data.code === 0) {
        const data = res.data.data;
        setNotifications(data.content || []);
        setTotalPages(data.totalPages || 0);
      }
    } catch (err) {
      console.error('Failed to fetch notifications', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications(page);
  }, [page, fetchNotifications]);

  const handleMarkAsRead = async (id: number) => {
    try {
      await api.put(`/api/notifications/${id}/read`);
      setNotifications(prev =>
        prev.map(n => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch (err) {
      console.error('Failed to mark as read', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await api.put('/api/notifications/read-all');
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (err) {
      console.error('Failed to mark all as read', err);
    }
  };

  const handleClickNotification = async (notification: NotificationItem) => {
    if (!notification.isRead) {
      await handleMarkAsRead(notification.id);
    }
    if (notification.projectId) {
      navigate(`/project/${notification.projectId}`);
    }
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (hours < 24) return `${hours}小时前`;
    if (days < 7) return `${days}天前`;
    return date.toLocaleDateString('zh-CN');
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">通知中心</h1>
        <Button variant="outline" size="sm" onClick={handleMarkAllAsRead}>
          全部标为已读
        </Button>
      </div>

      {loading ? (
        <div className="flex justify-center py-12 text-slate-500">加载中...</div>
      ) : notifications.length === 0 ? (
        <div className="flex flex-col items-center py-12 text-slate-400">
          <svg className="mb-3 h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
          <p>暂无通知</p>
        </div>
      ) : (
        <div className="space-y-2">
          {notifications.map(notification => (
            <div
              key={notification.id}
              className={`cursor-pointer rounded-lg border p-4 transition-colors hover:bg-slate-50 ${
                notification.isRead ? 'bg-white' : 'border-blue-200 bg-blue-50'
              }`}
              onClick={() => handleClickNotification(notification)}
            >
              <div className="mb-1 flex items-center gap-2">
                {!notification.isRead && (
                  <span className="inline-block h-2 w-2 rounded-full bg-blue-500" />
                )}
                <span
                  className={`rounded px-2 py-0.5 text-xs font-medium ${
                    typeColors[notification.type] || 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {typeLabels[notification.type] || notification.type}
                </span>
                <span className="text-xs text-slate-400">
                  {formatTime(notification.createdAt)}
                </span>
              </div>
              <h3 className="font-medium text-slate-900">{notification.title}</h3>
              {notification.content && (
                <p className="mt-1 text-sm text-slate-600">{notification.content}</p>
              )}
              {notification.relatedUserName && (
                <p className="mt-1 text-xs text-slate-400">
                  操作人: {notification.relatedUserName}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >
            上一页
          </Button>
          <span className="text-sm text-slate-600">
            {page + 1} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >
            下一页
          </Button>
        </div>
      )}
    </div>
  );
};

export default NotificationCenter;