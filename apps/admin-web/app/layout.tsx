import {RoleHeader} from "@/components/workspace/RoleHeader";
import "./role-workspaces.css";
import type { Metadata } from "next";
import "./globals.css";
import "./spatial-studio.css";

export const metadata: Metadata = {
  title: {default:"SurakshaAR · Learn. Explore. Prepare.",template:"%s · SurakshaAR"},
  referrer: "no-referrer",
  robots: {index:false,follow:false},
  description:
    "Practise safety decisions, explore equipment and manage institutional learning with SurakshaAR.",
  other: {
    "codex-preview": "development",
  },
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased spatial-light"><RoleHeader/>{children}</body>
    </html>
  );
}
