import { cn } from '@/lib/utils';

function Skeleton({ className, ...props }) {
  return (
    <div
      className={cn('rounded-[var(--radius)] shimmer-bg', className)}
      {...props}
    />
  );
}

export { Skeleton };
