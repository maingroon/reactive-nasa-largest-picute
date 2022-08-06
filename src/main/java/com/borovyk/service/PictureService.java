package com.borovyk.service;

import com.borovyk.entity.Picture;
import com.borovyk.entity.Pictures;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PictureService {

    @NonFinal
    @Value("${nasa.url}")
    String nasaUrl;

    @NonFinal
    @Value("${nasa.apiKey}")
    String nasaApiKey;

    WebClient pictureWebClient;

    public Mono<byte[]> findLargestPicture(String sol) {
        return WebClient.create()
                .get()
                .uri(buildNasaUrl(sol))
                .retrieve()
                .bodyToMono(Pictures.class)
                .flatMapMany(pictures -> Flux.fromIterable(pictures.pictures()))
                .flatMap(this::setPictureContentLength)
                .reduce((p1, p2) -> p1.getSize() > p2.getSize() ? p1 : p2)
                .flatMap(picture -> pictureWebClient
                        .get()
                        .uri(picture.getImgUrl())
                        .retrieve()
                        .bodyToMono(byte[].class));
    }

    private URI buildNasaUrl(String sol) {
        return UriComponentsBuilder.fromHttpUrl(nasaUrl)
                .queryParam("api_key", nasaApiKey)
                .queryParam("sol", sol)
                .build()
                .toUri();
    }

    private Mono<Picture> setPictureContentLength(Picture picture) {
        return pictureWebClient
                .head()
                .uri(picture.getImgUrl())
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    picture.setSize(response.getHeaders().getContentLength());
                    return picture;
                });
    }

}
