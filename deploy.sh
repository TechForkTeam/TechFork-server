#!/bin/bash

set -e

echo "🚀 Deployment script started..."

export DOCKER_IMAGE=${DOCKER_IMAGE}
export BRANCH=${BRANCH}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
export DB_URL=${DB_URL}
export DB_USERNAME=${DB_USERNAME}
export DB_PASSWORD=${DB_PASSWORD}
export REDIS_HOST=${REDIS_HOST}
export REDIS_PORT=${REDIS_PORT}
export REDIS_PASSWORD=${REDIS_PASSWORD}
export DISCORD_WEBHOOK_URL=${DISCORD_WEBHOOK_URL}
export ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}

echo "🐳 Pulling latest docker image..."
docker-compose pull

echo "🚀 Starting application with docker-compose..."
docker-compose up -d

echo "🧹 Pruning old docker images..."
docker image prune -af

echo "✅ Deployment completed successfully!"