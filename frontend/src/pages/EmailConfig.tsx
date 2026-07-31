import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import api from '../lib/api';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Mail, Send, Settings, CheckCircle, AlertCircle } from 'lucide-react';

type MailConfig = Record<string, string>;

const fieldLabels: Record<string, string> = {
  'mail.smtp.host': 'SMTP 服务器地址',
  'mail.smtp.port': 'SMTP 端口',
  'mail.smtp.username': '发信邮箱账号',
  'mail.smtp.password': '发信邮箱密码/授权码',
  'mail.smtp.ssl': '启用 SSL',
  'mail.from.address': '发信人地址',
  'mail.enabled': '启用邮件发送',
  'app.internal.url': '内网地址（邮件链接）',
  'app.external.url': '外网地址（邮件链接）',
};

const fieldPlaceholders: Record<string, string> = {
  'mail.smtp.host': 'smtp.qiye.163.com',
  'mail.smtp.port': '465',
  'mail.smtp.username': 'noreply@example.com',
  'mail.smtp.password': '留空则不修改密码',
  'mail.from.address': 'noreply@example.com',
  'app.internal.url': 'http://192.168.39.233:18080/',
  'app.external.url': 'http://343t787f48.wicp.vip/',
};

const isPasswordField = (key: string) => key === 'mail.smtp.password';

const EmailConfig: React.FC = () => {
  const [config, setConfig] = useState<MailConfig>({
    'mail.smtp.host': '',
    'mail.smtp.port': '',
    'mail.smtp.username': '',
    'mail.smtp.password': '',
    'mail.smtp.ssl': 'true',
    'mail.from.address': '',
    'mail.enabled': 'false',
    'app.internal.url': 'http://192.168.39.233:18080/',
    'app.external.url': 'http://343t787f48.wicp.vip/',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testEmail, setTestEmail] = useState('');
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  useEffect(() => {
    fetchConfig();
  }, []);

  const fetchConfig = async () => {
    try {
      const res = await api.get('/api/system/config/mail');
      if (res.data.code === 0) {
        setConfig(prev => ({ ...prev, ...(res.data.data as MailConfig) }));
      }
    } catch (err) {
      toast.error('获取邮件配置失败');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (key: string, value: string) => {
    setConfig(prev => ({ ...prev, [key]: value }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await api.put('/api/system/config/mail', config);
      if (res.data.code === 0) {
        toast.success('邮件配置已保存');
        await fetchConfig();
      } else {
        toast.error(res.data.message || '保存失败');
      }
    } catch (err) {
      toast.error('保存邮件配置失败');
    } finally {
      setSaving(false);
    }
  };

  const handleTest = async () => {
    if (!testEmail.trim()) {
      toast.error('请输入测试邮箱地址');
      return;
    }
    setTesting(true);
    setTestResult(null);
    try {
      const res = await api.post('/api/system/config/mail/test', { to: testEmail.trim() });
      if (res.data.code === 0) {
        setTestResult({ success: true, message: res.data.data || '测试邮件发送成功' });
      } else {
        setTestResult({ success: false, message: res.data.message || '测试邮件发送失败' });
      }
    } catch (err) {
      setTestResult({ success: false, message: '网络请求失败' });
    } finally {
      setTesting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12 text-slate-500">加载中...</div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <div className="mb-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => window.location.href = '/dashboard'}
        >
          <ChevronLeft className="mr-1 h-4 w-4" />
          返回首页
        </Button>
      </div>

      <h1 className="mb-6 text-2xl font-bold text-slate-900">
        <Mail className="mr-2 inline-block h-6 w-6" />
        邮件配置
      </h1>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Settings className="h-5 w-5" />
            SMTP 服务器配置
          </CardTitle>
          <p className="text-sm text-slate-500">
            提醒邮件会同时包含内网和外网地址，收件人可根据当前网络选择访问。
          </p>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {Object.keys(fieldLabels).map(key => (
              <div key={key}>
                <label className="mb-1 block text-sm font-medium text-slate-700">
                  {fieldLabels[key]}
                </label>
                {key === 'mail.smtp.ssl' || key === 'mail.enabled' ? (
                  <select
                    value={config[key] || 'false'}
                    onChange={e => handleChange(key, e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option value="true">启用</option>
                    <option value="false">关闭</option>
                  </select>
                ) : (
                  <Input
                    type={isPasswordField(key) ? 'password' : 'text'}
                    value={config[key] || ''}
                    onChange={e => handleChange(key, e.target.value)}
                    placeholder={fieldPlaceholders[key] || ''}
                  />
                )}
                {isPasswordField(key) && (
                  <p className="mt-1 text-xs text-slate-400">密码已脱敏显示，留空则不修改密码</p>
                )}
              </div>
            ))}
          </div>

          <div className="mt-6">
            <Button onClick={handleSave} disabled={saving}>
              {saving ? '保存中...' : '保存配置'}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Send className="h-5 w-5" />
            测试邮件发送
          </CardTitle>
          <p className="text-sm text-slate-500">
            配置保存后，可在此发送测试邮件验证配置是否正确
          </p>
        </CardHeader>
        <CardContent>
          <div className="flex gap-3">
            <Input
              type="email"
              value={testEmail}
              onChange={e => {
                setTestEmail(e.target.value);
                setTestResult(null);
              }}
              placeholder="输入测试邮箱地址"
              className="flex-1"
            />
            <Button onClick={handleTest} disabled={testing}>
              {testing ? '发送中...' : '发送测试邮件'}
            </Button>
          </div>

          {testResult && (
            <div className={`mt-4 flex items-start gap-2 rounded-lg p-3 text-sm ${
              testResult.success
                ? 'bg-green-50 text-green-700'
                : 'bg-red-50 text-red-600'
            }`}>
              {testResult.success ? (
                <CheckCircle className="mt-0.5 h-4 w-4 flex-shrink-0" />
              ) : (
                <AlertCircle className="mt-0.5 h-4 w-4 flex-shrink-0" />
              )}
              <span>{testResult.message}</span>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default EmailConfig;