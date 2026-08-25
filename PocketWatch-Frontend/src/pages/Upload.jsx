import { useState } from 'react'
import { Link } from 'react-router-dom'
import { UploadCloud, CheckCircle2, XCircle, FileText, Loader2, Landmark, History } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useToast } from '@/components/ui/toast'
import { useUploadCsv, useUploadStatus, useBankAccounts } from '@/api/queries'

const STAGE_WIDTH = { PENDING: '20%', PROCESSING: '65%', COMPLETED: '100%', FAILED: '100%' }

export default function Upload() {
  const { toast } = useToast()
  const [bankAccountId, setBankAccountId] = useState('')
  const [file, setFile] = useState(null)
  const [uploadId, setUploadId] = useState(null)
  const { data: accounts, isLoading: loadingAccounts } = useBankAccounts()
  const uploadCsv = useUploadCsv()
  const { data: status } = useUploadStatus(uploadId, { enabled: Boolean(uploadId) })

  const handleUpload = async () => {
    if (!file) return
    try {
      const result = await uploadCsv.mutateAsync({ file, bankAccountId: bankAccountId || undefined })
      setUploadId(result.id)
    } catch (err) {
      toast({
        title: 'Upload failed',
        description: err.response?.data?.message || 'Please check the file and try again.',
        variant: 'error',
      })
    }
  }

  const reset = () => {
    setFile(null)
    setUploadId(null)
  }

  const isDone = status?.status === 'COMPLETED'
  const isFailed = status?.status === 'FAILED'
  const width = STAGE_WIDTH[status?.status] ?? '10%'
  const hasAccounts = (accounts?.length ?? 0) > 0

  return (
    <div className="mx-auto w-full max-w-4xl">
      <div>
        <h1 className="font-display text-3xl">Upload statement</h1>
        <p className="text-fg/50 text-sm mt-1">
          Bring in a CSV or PDF export from your bank and we'll parse it into transactions.
        </p>
      </div>

      <Card className="p-6 space-y-5">
        <div>
          <label className="text-sm font-medium text-fg/70 mb-1.5 flex items-center gap-1.5">
            <Landmark size={14} /> Bank account
          </label>
          {loadingAccounts ? (
            <div className="h-10 rounded-md bg-white/5 animate-pulse" />
          ) : hasAccounts ? (
            <select
              value={bankAccountId}
              onChange={(e) => setBankAccountId(e.target.value)}
              className="w-full h-10 rounded-md border border-white/15 bg-elevated px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
            >
              <option value="">Select an account (optional)</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.nickname} · {a.bankName}
                </option>
              ))}
            </select>
          ) : (
            <p className="text-sm text-fg/50 bg-white/5 rounded-md px-3 py-2.5">
              No bank accounts yet.{' '}
              <Link to="/bank-accounts" className="text-fg underline underline-offset-4 font-medium">
                Add one
              </Link>{' '}
              to tag this upload — or continue without picking one.
            </p>
          )}
        </div>

        <label
          htmlFor="csv-input"
          className="flex flex-col items-center justify-center gap-2 border-2 border-dashed border-white/15 rounded-card py-10 cursor-pointer hover:border-brass hover:bg-brass/5 transition-colors"
        >
          <UploadCloud size={28} className="text-fg/40" />
          <p className="text-sm text-fg/60">
            {file ? (
              <span className="flex items-center gap-2 text-fg font-medium">
                <FileText size={15} /> {file.name}
              </span>
            ) : (
              'Click to choose a CSV or PDF statement, or drag it here'
            )}
          </p>
          <input
            id="csv-input"
            type="file"
            accept=".csv,.pdf"
            className="hidden"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
        </label>

        <Button
          variant="brass"
          size="lg"
          className="w-full gap-2"
          disabled={!file || uploadCsv.isPending}
          onClick={handleUpload}
        >
          {uploadCsv.isPending && <Loader2 size={16} className="animate-spin" />}
          {uploadCsv.isPending ? 'Uploading…' : 'Upload and process'}
        </Button>

        {uploadId && (
          <div className="border-t border-white/10 pt-5">
            {isFailed ? (
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-debit text-sm">
                  <XCircle size={18} />
                  {status?.errorMessage || 'Processing failed. Check the file format and try again.'}
                </div>
                <Button variant="outline" size="sm" onClick={reset}>
                  Try another file
                </Button>
              </div>
            ) : isDone ? (
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-credit text-sm font-medium">
                  <CheckCircle2 size={18} />
                  Done — extracted {status?.transactionsExtracted ?? 0} transactions.
                </div>
                <div className="flex gap-2">
                  <Button asChild variant="brass" size="sm">
                    <Link to="/transactions">Review transactions</Link>
                  </Button>
                  <Button variant="outline" size="sm" onClick={reset}>
                    Upload another
                  </Button>
                </div>
              </div>
            ) : (
              <div>
                <div className="flex items-center justify-between text-sm text-fg/60 mb-2">
                  <span>
                    {status?.status === 'PROCESSING' ? 'Processing your statement…' : 'Queued…'}
                  </span>
                  <span className="font-mono text-xs uppercase">{status?.status ?? 'PENDING'}</span>
                </div>
                <div className="h-2 rounded-full bg-white/10 overflow-hidden">
                  <div className="h-full bg-brass transition-all duration-500" style={{ width }} />
                </div>
              </div>
            )}
          </div>
        )}
      </Card>

      <Link to="/upload-history" className="flex items-center gap-1.5 text-sm text-fg/50 hover:text-fg w-fit">
        <History size={14} /> View past uploads
      </Link>
    </div>
  )
}
