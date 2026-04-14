// client/gateway_client_test.go
package client_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	
	"github.com/Riviya/Keypilot/cli/client"
    "github.com/Riviya/Keypilot/cli/model"
)

// ---------------------------------------------------------------
// ADD KEY
// ---------------------------------------------------------------

func TestAddKey_Success(t *testing.T) {
	// Arrange — spin up a fake gateway server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Assert the correct HTTP method and path
		if r.Method != http.MethodPost {
			t.Errorf("expected POST, got %s", r.Method)
		}
		if r.URL.Path != "/api/keys" {
			t.Errorf("expected /api/keys, got %s", r.URL.Path)
		}

		// Assert request body contains provider and keyValue
		var body map[string]string
		json.NewDecoder(r.Body).Decode(&body)
		if body["provider"] != "openai" {
			t.Errorf("expected provider=openai, got %s", body["provider"])
		}

		// Respond with a fake created key
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(model.ApiKeyResponse{
			ID:       "uuid-123",
			Provider: "openai",
			Active:   true,
		})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)

	// Act
	resp, err := c.AddKey("openai", "sk-test-abc")

	// Assert
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if resp.ID != "uuid-123" {
		t.Errorf("expected id=uuid-123, got %s", resp.ID)
	}
	if resp.Provider != "openai" {
		t.Errorf("expected provider=openai, got %s", resp.Provider)
	}
	if !resp.Active {
		t.Error("expected active=true")
	}
}

func TestAddKey_ServerError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)

	_, err := c.AddKey("openai", "sk-bad")

	if err == nil {
		t.Fatal("expected error on 500 response, got nil")
	}
}

func TestAddKey_BadRequest(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "provider must not be blank",
		})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)

	_, err := c.AddKey("", "sk-test")

	if err == nil {
		t.Fatal("expected error on 400 response, got nil")
	}
}

// ---------------------------------------------------------------
// LIST KEYS
// ---------------------------------------------------------------

func TestListKeys_ReturnsAllKeys(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			t.Errorf("expected GET, got %s", r.Method)
		}
		if r.URL.Path != "/api/keys" {
			t.Errorf("expected /api/keys, got %s", r.URL.Path)
		}

		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode([]model.ApiKeyResponse{
			{ID: "uuid-1", Provider: "openai", Active: true},
			{ID: "uuid-2", Provider: "anthropic", Active: true},
		})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)

	keys, err := c.ListKeys()

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if len(keys) != 2 {
		t.Errorf("expected 2 keys, got %d", len(keys))
	}
	if keys[0].Provider != "openai" {
		t.Errorf("expected openai, got %s", keys[0].Provider)
	}
}

func TestListKeys_ReturnsEmptyList(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode([]model.ApiKeyResponse{})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)

	keys, err := c.ListKeys()

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if len(keys) != 0 {
		t.Errorf("expected 0 keys, got %d", len(keys))
	}
}

// ---------------------------------------------------------------
// DELETE KEY
// ---------------------------------------------------------------

func TestDeleteKey_Success(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodDelete {
			t.Errorf("expected DELETE, got %s", r.Method)
		}
		if r.URL.Path != "/api/keys/uuid-123" {
			t.Errorf("expected /api/keys/uuid-123, got %s", r.URL.Path)
		}
		w.WriteHeader(http.StatusNoContent)
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)

	err := c.DeleteKey("uuid-123")

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
}

func TestDeleteKey_NotFound(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "No API key found with id: ghost-id",
		})
	}))
	defer server.Close()

	c := client.NewGatewayClient(server.URL)

	err := c.DeleteKey("ghost-id")

	if err == nil {
		t.Fatal("expected error for 404, got nil")
	}
}