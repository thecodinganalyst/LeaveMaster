type LeaveMaestroLogoProps = {
  className?: string;
  showWordmark?: boolean;
};

export function LeaveMaestroLogo({ className = '', showWordmark = true }: LeaveMaestroLogoProps) {
  return (
    <span className={`inline-flex items-center gap-2.5 text-brand-600 ${className}`}>
      <svg
        viewBox="0 0 64 64"
        className="h-9 w-9 shrink-0"
        aria-hidden="true"
        focusable="false"
      >
        <path
          d="M10 15v36h16"
          fill="none"
          stroke="currentColor"
          strokeWidth="6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M25 50V31l10 10 11-12v21"
          fill="none"
          stroke="currentColor"
          strokeWidth="6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle cx="31" cy="19" r="4.5" fill="currentColor" />
        <path
          d="M23 28c5-5 10-6 15-3 3 2 5 4 8 4"
          fill="none"
          stroke="currentColor"
          strokeWidth="4.5"
          strokeLinecap="round"
        />
        <path
          d="M42 27l11-15"
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
        />
        <circle cx="54" cy="10.5" r="2.5" fill="currentColor" />
      </svg>
      {showWordmark ? <span className="text-lg font-semibold tracking-tight text-slate-950">LeaveMaestro</span> : null}
    </span>
  );
}
