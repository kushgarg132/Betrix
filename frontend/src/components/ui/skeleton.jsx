import { cn } from '@/lib/utils';

function Skeleton({ className, ...props }) {
  return (
    <div
      className={cn('rounded-[var(--radius)] bg-surface-elevated animate-pulse', className)}
      {...props}
    />
  );
}

export { Skeleton };
