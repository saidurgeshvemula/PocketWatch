import { motion } from 'framer-motion'

function FloatingPaths({ position }) {
  const paths = Array.from({ length: 30 }, (_, i) => ({
    id: i,
    d: `M-${380 - i * 5 * position} -${189 + i * 6}C-${380 - i * 5 * position} -${189 + i * 6} -${
      312 - i * 5 * position
    } ${216 - i * 6} ${152 - i * 5 * position} ${343 - i * 6}C${616 - i * 5 * position} ${
      470 - i * 6
    } ${684 - i * 5 * position} ${875 - i * 6} ${684 - i * 5 * position} ${875 - i * 6}`,
    width: 0.4 + i * 0.025,
  }))

  return (
    <div className="absolute inset-0 pointer-events-none">
      <svg className="w-full h-full text-brass" viewBox="0 0 696 316" fill="none">
        <title>Cash flow lines</title>
        {paths.map((path) => (
          <motion.path
            key={path.id}
            d={path.d}
            stroke="currentColor"
            strokeWidth={path.width}
            strokeOpacity={0.08 + path.id * 0.02}
            initial={{ pathLength: 0.3, opacity: 0.4 }}
            animate={{
              pathLength: 1,
              opacity: [0.2, 0.45, 0.2],
              pathOffset: [0, 1, 0],
            }}
            transition={{
              duration: 22 + Math.random() * 10,
              repeat: Number.POSITIVE_INFINITY,
              ease: 'linear',
            }}
          />
        ))}
      </svg>
    </div>
  )
}

export function BackgroundPaths({ children }) {
  return (
    <div className="relative min-h-screen w-full flex items-center justify-center overflow-hidden bg-canvas">
      <div className="absolute inset-0">
        <FloatingPaths position={1} />
        <FloatingPaths position={-1} />
      </div>
      <div className="relative z-10 w-full px-4">{children}</div>
    </div>
  )
}
