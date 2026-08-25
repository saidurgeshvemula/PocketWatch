import { Outlet } from "react-router-dom";
import { Dock } from "./Dock";

export function AppLayout() {
  return (
    <div className="min-h-screen bg-canvas">
      <Dock />

      <main className="ledger-scrollbar min-h-screen overflow-y-auto pt-20 pb-28 px-6 sm:px-10">
        <Outlet />
      </main>
    </div>
  );
}