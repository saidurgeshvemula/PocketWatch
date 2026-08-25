export function PageContainer({
  children,
  size = "lg",
}) {
  const widths = {
    sm: "max-w-2xl",
    md: "max-w-4xl",
    lg: "max-w-6xl",
    xl: "max-w-7xl",
    full: "max-w-none",
  };

  return (
    <div className={`mx-auto w-full ${widths[size]}`}>
      {children}
    </div>
  );
}