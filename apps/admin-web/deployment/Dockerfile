FROM node:24-bookworm-slim AS build
WORKDIR /work
COPY source/apps/admin-web/package*.json ./apps/admin-web/
WORKDIR /work/apps/admin-web
RUN npm ci --include=dev --include=optional --no-audit --no-fund
WORKDIR /work
COPY source/ ./
WORKDIR /work/apps/admin-web
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build
FROM node:24-bookworm-slim AS runtime
ENV NODE_ENV=production HOSTNAME=0.0.0.0 PORT=5173 NEXT_TELEMETRY_DISABLED=1
WORKDIR /app
COPY --from=build --chown=node:node /work/apps/admin-web/.next/standalone ./
COPY --from=build --chown=node:node /work/apps/admin-web/.next/static ./apps/admin-web/.next/static
COPY --from=build --chown=node:node /work/apps/admin-web/public ./apps/admin-web/public
COPY --chown=node:node source/apps/admin-web/drizzle ./drizzle
COPY --chown=node:node source/apps/admin-web/scripts/migrate-node.mjs source/apps/admin-web/scripts/backup-node.mjs source/apps/admin-web/scripts/maintain-node.mjs ./scripts/
USER node
EXPOSE 5173
CMD ["node","apps/admin-web/server.js"]
