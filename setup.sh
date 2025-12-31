#!/bin/bash

# Support Portal Setup Script
echo "🚀 Setting up Support Portal Application"
echo "========================================"

# Check if .env exists
if [ -f ".env" ]; then
    echo "⚠️  .env file already exists. Skipping creation."
else
    echo "📋 Creating .env file from template..."
    cp env.example .env
    echo "✅ .env file created. Please edit it with your configuration."
    echo "   Edit .env file: nano .env"
fi

echo ""
echo "📦 Building application..."
./mvnw clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "🎯 Available commands:"
    echo "  • Run with H2 (default):    ./mvnw spring-boot:run"
    echo "  • Run with PostgreSQL:     ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres"
    echo "  • Docker Compose (dev):     docker-compose up --build"
    echo "  • Docker build:             docker build -t support-portal ."
    echo ""
    echo "🌐 Application will be available at: http://localhost:8081"
    echo "📚 API Documentation: http://localhost:8081/swagger-ui/index.html"
else
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi
