import * as React from 'react';
import * as SliderPrimitive from '@radix-ui/react-slider';
import { cn } from '@/lib/utils';

const Slider = React.forwardRef(({ className, ...props }, ref) => (
  <SliderPrimitive.Root
    ref={ref}
    className={cn('relative flex w-full touch-none select-none items-center', className)}
    {...props}
  >
    <SliderPrimitive.Track className="relative h-2 w-full grow overflow-hidden rounded-full bg-surface-elevated">
      <SliderPrimitive.Range className="absolute h-full bg-gold" />
    </SliderPrimitive.Track>
    <SliderPrimitive.Thumb
      className={cn(
        'block h-8 w-8 rounded-full border-2 border-gold bg-surface-elevated shadow-md ring-offset-background',
        'transition-all duration-150',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2',
        'disabled:pointer-events-none disabled:opacity-50',
        'hover:bg-surface-overlay hover:scale-110 active:scale-95',
        'cursor-grab active:cursor-grabbing'
      )}
      aria-label="Raise amount"
    />
  </SliderPrimitive.Root>
));
Slider.displayName = SliderPrimitive.Root.displayName;

export { Slider };
