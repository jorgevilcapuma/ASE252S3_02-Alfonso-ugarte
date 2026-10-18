import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RegistrarAsistenciaComponent } from './registrar-asistencia.component';

describe('RegistrarAsistenciaComponent', () => {
  let component: RegistrarAsistenciaComponent;
  let fixture: ComponentFixture<RegistrarAsistenciaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrarAsistenciaComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrarAsistenciaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});