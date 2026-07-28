import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/api';

interface NotificationItem {
  id: number;
  type: string;
  title: string;
  content: string;
  projectId: number | null;
  isRead: boolean;
  createdAt: string;
}

const POLL_INTERVAL = 30000; // 30秒轮询

const NotificationBell: React.FC = () => {
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchUnread = useCallback(async () => {
    try {
      const [countRes, listRes] = await Promise.all([
        api.get('/api/notifications/count-unread'),
        api.get('/api/notifications/unread'),
      ]);
      if (countRes.data.code === 0) {
        setUnreadCount(countRes.data.data || 0);
      }
      if (listRes.data.code === 0) {
        setNotifications(listRes.data.data || []);
      }
    } catch (err) {
      // silently ignore polling errors
    }
  }, []);

  useEffect(() => {
    fetchUnread();
    const interval = setInterval(fetchUnread, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchUnread]);

  // 点击外部关闭下拉面板
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    if (open) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  const handleToggle = () => {
    setOpen(prev => !prev);
    if (!open) {
      fetchUnread();
    }
  };

  const handleClickNotification = async (notification: NotificationItem) => {
    try {
      await api.put(`/api/notifications/${notification.id}/read`);
      setUnreadCount(prev => Math.max(0, prev - 1));
      setNotifications(prev => prev.map(n =>
        n.id === notification.id ? { ...n, isRead: true } : n
      ));
    } catch (err) {
      // ignore
    }
    setOpen(false);
    if (notification.projectId) {
      navigate(`/project/${notification.projectId}`);
    }
  };

  const handleViewAll = () => {
    setOpen(false);
    navigate('/notifications');
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

  const typeLabels: Record<string, string> = {
    PROJECT_COMPLETION: '项目完善',
    DELIVERABLE_UPLOADED: '交付物上传',
    REVIEW_SUBMITTED: '评审提交',
    REVIEW_DECIDED: '评审结果',
    MILESTONE_APPROVED: '里程碑通过',
    REVIEW_REJECTED: '评审退回',
  };

  return (
    <div ref={dropdownRef} className="relative">
      <button
        onClick={handleToggle}
        className="relative rounded-full p-2 text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900"
        title="通知"
      >
        <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1 text-xs font-bold text-white">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-80 rounded-lg border bg-white shadow-lg z-50">
          <div className="flex items-center justify-between border-b px-4 py-3">
            <h3 className="text-sm font-semibold text-slate-900">通知</h3>
            {notifications.length > 0 && (
              <button
                onClick={handleViewAll}
                className="text-xs text-blue-600 hover:text-blue-800"
              >
                查看全部
              </button>
            )}
          </div>
          <div className="max-h-96 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="px-4 py-8 text-center text-sm text-slate-400">
                暂无未读通知
              </div>
            ) : (
              notifications.map(notification => (
                <div
                  key={notification.id}
                  className="cursor-pointer border-b px-4 py-3 transition-colors hover:bg-slate-50 last:border-b-0"
                  onClick={() => handleClickNotification(notification)}
                >
                  <div className="flex items-start gap-2">
                    <span className="mt-0.5 inline-block h-2 w-2 flex-shrink-0 rounded-full bg-blue-500" />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-medium text-slate-500">
                          {typeLabels[notification.type] || notification.type}
                        </span>
                        <span className="text-xs text-slate-400">
                          {formatTime(notification.createdAt)}
                        </span>
                      </div>
                      <p className="mt-0.5 text-sm font-medium text-slate-900 truncate">
                        {notification.title}
                      </p>
                      {notification.content && (
                        <p className="mt-0.5 text-xs text-slate-500 line-clamp-2">
                          {notification.content}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;