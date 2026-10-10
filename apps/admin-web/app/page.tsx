import { getAppUser } from "@/lib/auth";
import Dashboard from "@/components/training/Dashboard";
import {LearningStudio} from "@/components/studio/LearningStudio";
export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getAppUser();
  return user ? <Dashboard /> : <LearningStudio user={null} />;
}
