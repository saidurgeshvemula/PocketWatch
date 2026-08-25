import { useEffect, useRef, useState } from 'react'
import { Send, Loader2 } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useChatHistory, useSendChatMessage } from '@/api/queries'
import { cn } from '@/lib/utils'

export default function Chat() {
  const { data: history, isLoading } = useChatHistory()
  const sendMessage = useSendChatMessage()

  const [draft, setDraft] = useState('')
  const bottomRef = useRef(null)

  const messages = history ?? []

  useEffect(() => {
    bottomRef.current?.scrollIntoView({
      behavior: 'smooth',
    })
  }, [messages.length, sendMessage.isPending])

   const handleSend = (e) => {
    e.preventDefault()

    const text = draft.trim()

    if (!text) return

    setDraft('')

    sendMessage.mutate({
      message: text,
    })
  }
  return (
    <div className="mx-auto w-full max-w-5xl h-[calc(100vh-4rem)] flex flex-col">

      <div className="mb-4">
        <h1 className="font-display text-3xl">
          AI Chat
        </h1>

        <p className="text-fg/50 text-sm mt-1">
          Ask about your spending, budgets, or specific transactions.
        </p>
      </div>

      <div className="flex-1 rounded-card bg-card/80 border border-white/5 shadow-card overflow-y-auto ledger-scrollbar p-4 space-y-3">

        {isLoading ? (

          <p className="text-center text-fg/40 text-sm py-8">
            Loading conversation…
          </p>

        ) : messages.length === 0 ? (

          <p className="text-center text-fg/40 text-sm py-8">
            Say hello to get started.
          </p>

        ) : (

          messages.map((m) => (
            <div
              key={m.id}
              className={cn(
                'flex',
                m.role === 'USER'
                  ? 'justify-end'
                  : 'justify-start'
              )}
            >

              <div
                className={cn(
                  'px-4 py-2.5 rounded-2xl text-sm leading-relaxed',

                  m.role === 'USER'
                    ? 'max-w-[75%] bg-canvas text-fg rounded-br-sm'
                    : 'max-w-[90%] bg-credit-light text-fg rounded-bl-sm'
                )}
              >

                {m.role === 'USER' ? (

                  m.content

                ) : (

                  <div
                    className={cn(
                      'prose prose-sm prose-invert max-w-none',

                      'prose-headings:font-display prose-headings:text-fg prose-headings:mb-1.5 prose-headings:mt-3 first:prose-headings:mt-0',

                      'prose-h2:text-sm prose-h2:uppercase prose-h2:tracking-wide prose-h2:text-fg/70 prose-h2:font-semibold',

                      'prose-p:my-1.5 prose-p:leading-relaxed',

                      'prose-ul:my-1.5 prose-ul:pl-4 prose-li:my-0.5 prose-li:marker:text-brass',

                      'prose-ol:my-1.5 prose-ol:pl-4',

                      'prose-strong:text-brass prose-strong:font-semibold',

                      'prose-table:my-2 prose-table:text-xs',

                      'prose-th:border prose-th:border-white/10 prose-th:px-2 prose-th:py-1 prose-th:bg-white/5 prose-th:text-fg/80',

                      'prose-td:border prose-td:border-white/10 prose-td:px-2 prose-td:py-1',

                      'prose-hr:border-white/10 prose-hr:my-2'
                    )}
                  >
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                    >
                      {m.content}
                    </ReactMarkdown>
                  </div>

                )}

              </div>

            </div>
          ))
        )}

        {sendMessage.isPending && (

          <div className="flex justify-start">

            <div className="bg-credit-light text-fg px-4 py-2.5 rounded-2xl rounded-bl-sm flex items-center gap-2 text-sm">

              <Loader2
                size={14}
                className="animate-spin"
              />

              thinking…

            </div>

          </div>
        )}

        <div ref={bottomRef} />

      </div>

      <form
        onSubmit={handleSend}
        className="mt-3 flex gap-2"
      >

        <input
          type="text"
          value={draft}
          onChange={(e) =>
            setDraft(e.target.value)
          }
          placeholder="Message PocketWatch AI…"
          className="flex-1 h-11 rounded-full border border-white/15 bg-elevated px-4 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass"
        />

        <button
          type="submit"
          disabled={
            !draft.trim() ||
            sendMessage.isPending
          }
          className="h-11 w-11 shrink-0 rounded-full bg-brass text-fg flex items-center justify-center disabled:opacity-50 hover:bg-brass-light transition-colors"
        >
          <Send size={17} />
        </button>

      </form>

    </div>
  )
}