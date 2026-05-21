import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Toaster } from 'sonner';
import Dashboard from './pages/Dashboard';
import ProjectDetail from './pages/ProjectDetail';
import PmcApproval from './pages/PmcApproval';
import CreateProject from './pages/CreateProject';
import Timeline from './pages/Timeline';
import Login from './pages/Login';
import AccountManagement from './pages/AccountManagement';
import ReviewCenter from './pages/ReviewCenter';
import { Button } from './components/ui/button';

const AppContent: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleAccountManagement = () => {
    navigate('/account-management');
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b bg-white/90 px-4 py-3 shadow-sm">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <div className="text-lg font-semibold text-slate-900">项目管理系统</div>
          {isAuthenticated ? (
            <div className="flex items-center gap-4 text-sm text-slate-700">
              <div>
                <span className="font-medium">{user?.username}</span>
                {user?.department ? ` | ${user.department}` : ''}
              </div>
              <Button variant="outline" size="sm" onClick={handleAccountManagement}>
                账号管理
              </Button>
              <Button variant="outline" size="sm" onClick={logout}>
                退出登录
              </Button>
            </div>
          ) : (
            <div className="text-sm text-slate-600">请登录以访问仪表盘和项目详情</div>
          )}
        </div>
      </header>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/project/:projectId"
          element={
            <ProtectedRoute>
              <ProjectDetail />
            </ProtectedRoute>
          }
        />
        <Route
          path="/project/:projectId/approval"
          element={
            <ProtectedRoute roles={['ROLE_PMC']}>
              <PmcApproval />
            </ProtectedRoute>
          }
        />
        <Route
          path="/projects/:projectId/timeline"
          element={
            <ProtectedRoute>
              <Timeline />
            </ProtectedRoute>
          }
        />
        <Route
          path="/create-project"
          element={
            <ProtectedRoute>
              <CreateProject />
            </ProtectedRoute>
          }
        />
        <Route
          path="/review-center"
          element={
            <ProtectedRoute>
              <ReviewCenter />
            </ProtectedRoute>
          }
        />
        <Route
          path="/account-management"
          element={
            <ProtectedRoute>
              <AccountManagement />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
        <Route path="*" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
      </Routes>
      <Toaster />
    </div>
  );
};

const App: React.FC = () => (
  <AuthProvider>
    <Router>
      <AppContent />
    </Router>
  </AuthProvider>
);

export default App;